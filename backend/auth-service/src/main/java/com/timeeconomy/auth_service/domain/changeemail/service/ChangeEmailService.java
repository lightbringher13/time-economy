package com.timeeconomy.auth_service.domain.changeemail.service;

import com.timeeconomy.auth_service.domain.auth.model.AuthUser;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth_service.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth_service.domain.changeemail.model.SecondFactorType;
import com.timeeconomy.auth_service.domain.changeemail.port.in.RequestEmailChangeUseCase;
import com.timeeconomy.auth_service.domain.changeemail.port.in.VerifyNewEmailCodeUseCase;
import com.timeeconomy.auth_service.domain.changeemail.port.in.VerifySecondFactorUseCase;
import com.timeeconomy.auth_service.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth_service.domain.common.lock.port.DistributedLockPort;
import com.timeeconomy.auth_service.domain.common.lock.port.LockHandle;
import com.timeeconomy.auth_service.domain.common.notification.port.EmailNotificationPort;
import com.timeeconomy.auth_service.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth_service.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailChangeEmailMismatchException;
import com.timeeconomy.auth_service.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth_service.domain.exception.InvalidCurrentPasswordException;
import com.timeeconomy.auth_service.domain.exception.InvalidNewEmailCodeException;
import com.timeeconomy.auth_service.domain.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth_service.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth_service.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth_service.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth_service.domain.verification.port.in.VerificationChallengeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChangeEmailService implements
        RequestEmailChangeUseCase,
        VerifyNewEmailCodeUseCase,
        VerifySecondFactorUseCase {

    private static final long CHANGE_EMAIL_TTL_MINUTES = 30L;

    // OTP policy (you can tune later)
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final EmailNotificationPort emailNotificationPort;
    private final DistributedLockPort distributedLockPort;

    private final VerificationChallengeUseCase verificationChallengeUseCase;

    // ============ 1) Request email change ============

    @Override
    @Transactional
    public RequestEmailChangeResult requestEmailChange(RequestEmailChangeCommand command) {
        LocalDateTime now = LocalDateTime.now();

        Long userId = command.userId();
        String currentPassword = command.currentPassword();
        String newEmailRaw = command.newEmail();

        AuthUser user = authUserRepositoryPort.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));

        if (!passwordEncoderPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        String newEmail = newEmailRaw.trim().toLowerCase();

        authUserRepositoryPort.findByEmail(newEmail).ifPresent(existing -> {
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // cancel active request
        emailChangeRequestRepositoryPort.findActiveByUserId(userId)
                .ifPresent(existing -> {
                    existing.markCanceled(now);
                    emailChangeRequestRepositoryPort.save(existing);
                });

        // create request aggregate (codes no longer stored here)
        LocalDateTime expiresAt = now.plusMinutes(CHANGE_EMAIL_TTL_MINUTES);
        EmailChangeRequest request = EmailChangeRequest.create(
                userId,
                user.getEmail(),
                newEmail,
                null,
                expiresAt,
                now
        );

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        // create OTP challenge for NEW email
        verificationChallengeUseCase.createOtp(new VerificationChallengeUseCase.CreateOtpCommand(
                VerificationSubjectType.USER,
                userId.toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL,
                newEmail,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null, // requestIp (optional; pass from controller later)
                null  // userAgent (optional)
        ));

        return new RequestEmailChangeResult(
                saved.getId(),
                maskEmail(newEmail)
        );
    }

    // ============ 2) Verify code sent to NEW email ============

    @Override
    @Transactional
    public VerifyNewEmailCodeResult verifyNewEmailCode(VerifyNewEmailCodeCommand command) {
        LocalDateTime now = LocalDateTime.now();

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(
                        command.userId(),
                        command.requestId()
                ));

        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidNewEmailCodeException();
        }

        if (request.getStatus() != EmailChangeStatus.PENDING) {
            throw new InvalidNewEmailCodeException();
        }

        // verify NEW-email OTP via challenge service
        var verify = verificationChallengeUseCase.verifyOtp(new VerificationChallengeUseCase.VerifyOtpCommand(
                VerificationSubjectType.USER,
                command.userId().toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL,
                request.getNewEmail(),
                command.code()
        ));

        if (!verify.success()) {
            throw new InvalidNewEmailCodeException();
        }

        // mark request state
        request.markNewEmailVerified(now);

        // decide second factor
        AuthUser user = authUserRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

        SecondFactorType type;
        if (user.isPhoneVerified() && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            type = SecondFactorType.PHONE;

            verificationChallengeUseCase.createOtp(new VerificationChallengeUseCase.CreateOtpCommand(
                    VerificationSubjectType.USER,
                    command.userId().toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_PHONE,
                    VerificationChannel.SMS,
                    user.getPhoneNumber(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));
        } else {
            type = SecondFactorType.OLD_EMAIL;

            verificationChallengeUseCase.createOtp(new VerificationChallengeUseCase.CreateOtpCommand(
                    VerificationSubjectType.USER,
                    command.userId().toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_OLD_EMAIL,
                    VerificationChannel.EMAIL,
                    request.getOldEmail(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));
        }

        request.setSecondFactorType(type, now);

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        return new VerifyNewEmailCodeResult(
                saved.getId(),
                type
        );
    }

    // ============ 3) Verify second factor AND commit ============

    @Override
    @Transactional
    public VerifySecondFactorResult verifySecondFactorAndCommit(VerifySecondFactorCommand command) {
        LocalDateTime now = LocalDateTime.now();
        String lockKey = "change-email:user:" + command.userId();

        try (LockHandle lock = distributedLockPort.acquireLock(lockKey)) {

            EmailChangeRequest request = emailChangeRequestRepositoryPort
                    .findByIdAndUserId(command.requestId(), command.userId())
                    .orElseThrow(() -> new EmailChangeRequestNotFoundException(
                            command.userId(),
                            command.requestId()
                    ));

            if (request.isExpired(now)) {
                request.markExpired(now);
                emailChangeRequestRepositoryPort.save(request);
                throw new InvalidSecondFactorCodeException();
            }

            if (request.getStatus() != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
                throw new InvalidSecondFactorCodeException();
            }

            SecondFactorType type = request.getSecondFactorType();
            if (type == null) {
                throw new InvalidSecondFactorCodeException();
            }

            boolean secondOk;

            if (type == SecondFactorType.PHONE) {
                AuthUser user = authUserRepositoryPort.findById(command.userId())
                        .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

                String phone = user.getPhoneNumber();
                if (phone == null || phone.isBlank()) {
                    throw new InvalidSecondFactorCodeException();
                }

                var verify = verificationChallengeUseCase.verifyOtp(new VerificationChallengeUseCase.VerifyOtpCommand(
                        VerificationSubjectType.USER,
                        command.userId().toString(),
                        VerificationPurpose.CHANGE_EMAIL_2FA_PHONE,
                        VerificationChannel.SMS,
                        phone,
                        command.code()
                ));
                secondOk = verify.success();

            } else { // OLD_EMAIL
                var verify = verificationChallengeUseCase.verifyOtp(new VerificationChallengeUseCase.VerifyOtpCommand(
                        VerificationSubjectType.USER,
                        command.userId().toString(),
                        VerificationPurpose.CHANGE_EMAIL_2FA_OLD_EMAIL,
                        VerificationChannel.EMAIL,
                        request.getOldEmail(),
                        command.code()
                ));
                secondOk = verify.success();
            }

            if (!secondOk) {
                throw new InvalidSecondFactorCodeException();
            }

            request.markReadyToCommit(now);

            AuthUser user = authUserRepositoryPort.findById(command.userId())
                    .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

            if (!user.getEmail().equals(request.getOldEmail())) {
                throw new EmailChangeEmailMismatchException();
            }

            user.updateEmail(request.getNewEmail(), now);
            authUserRepositoryPort.save(user);

            request.markCompleted(now);
            EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

            // notify old & new email
            emailNotificationPort.notifyEmailChangedOldEmail(
                    request.getOldEmail(),
                    request.getNewEmail()
            );
            emailNotificationPort.notifyEmailChangedNewEmail(
                    request.getNewEmail()
            );

            return new VerifySecondFactorResult(
                    saved.getId(),
                    request.getNewEmail()
            );
        }
    }

    // ============ helpers ============

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return "***" + email.substring(atIdx);
        return email.charAt(0) + "***" + email.substring(atIdx);
    }
}