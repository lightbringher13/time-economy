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
import com.timeeconomy.auth_service.domain.emailverification.port.out.EmailVerificationMailPort;
import com.timeeconomy.auth_service.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailChangeEmailMismatchException;
import com.timeeconomy.auth_service.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth_service.domain.exception.InvalidCurrentPasswordException;
import com.timeeconomy.auth_service.domain.exception.InvalidNewEmailCodeException;
import com.timeeconomy.auth_service.domain.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth_service.domain.phoneverification.port.out.PhoneVerificationSmsPort;
import com.timeeconomy.auth_service.domain.port.out.PasswordEncoderPort;
import com.timeeconomy.auth_service.domain.port.out.DistributedLockPort;
import com.timeeconomy.auth_service.domain.port.out.EmailNotificationPort;
import com.timeeconomy.auth_service.domain.port.out.LockHandle;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ChangeEmailService implements
        RequestEmailChangeUseCase,
        VerifyNewEmailCodeUseCase,
        VerifySecondFactorUseCase {

    private static final long CHANGE_EMAIL_TTL_MINUTES = 30L;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final EmailVerificationMailPort emailVerificationMailPort;
    private final EmailNotificationPort emailNotificationPort;
    private final PhoneVerificationSmsPort phoneVerificationSmsPort;
    private final DistributedLockPort distributedLockPort;

    private final SecureRandom random = new SecureRandom();

    // ============ 1) Request email change ============

    @Override
    @Transactional
    public RequestEmailChangeResult requestEmailChange(RequestEmailChangeCommand command) {
        LocalDateTime now = LocalDateTime.now();

        Long authUserId = command.userId();
        String currentPassword = command.currentPassword();
        String newEmail = command.newEmail();

        AuthUser user = authUserRepositoryPort.findById(authUserId)
                .orElseThrow(() -> new AuthUserNotFoundException(authUserId));

        if (!passwordEncoderPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        authUserRepositoryPort.findByEmail(newEmail).ifPresent(existing -> {
            // 여기서는 "이미 가입된 이메일입니다. 로그인 또는 비밀번호 찾기를 사용하세요" 같은 메시지로 매핑 가능
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // 3. Cancel active existing request (if any)
        emailChangeRequestRepositoryPort.findActiveByUserId(command.userId())
                .ifPresent(existing -> {
                    existing.markCanceled(now);
                    emailChangeRequestRepositoryPort.save(existing);
                });

        // 4. Create new change-email request aggregate
        LocalDateTime expiresAt = now.plusMinutes(CHANGE_EMAIL_TTL_MINUTES);
        String newEmailCode = generateCode();

        EmailChangeRequest request = EmailChangeRequest.create(
                command.userId(),
                user.getEmail(),
                command.newEmail(),
                newEmailCode,
                expiresAt,
                now
        );

        // 5. Persist
        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        // 6. Send verification code to NEW email
        emailVerificationMailPort.sendVerificationCode(
                command.newEmail(),
                newEmailCode
        );

        return new RequestEmailChangeResult(
                saved.getId(),
                maskEmail(command.newEmail())
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

        // Let domain handle state & expiry checks
        request.verifyNewEmailCode(command.code(), now);

        if (request.getStatus() != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
                throw new InvalidNewEmailCodeException();
            }

        // Decide which second factor to use
        AuthUser user = authUserRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));
        SecondFactorType type;

        if (user.isPhoneVerified()) {
            type = SecondFactorType.PHONE;
            String smsCode = generateCode();
            request.setSecondFactor(type, smsCode, now);

            phoneVerificationSmsPort.sendVerificationCode(
                "+82",
                    user.getPhoneNumber(),
                    smsCode
            );
        } else {
            type = SecondFactorType.OLD_EMAIL;
            String oldEmailCode = generateCode();
            request.setSecondFactor(type, oldEmailCode, now);

            emailVerificationMailPort.sendVerificationCode(
                    request.getOldEmail(),
                    oldEmailCode
            );
        }

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        return new VerifyNewEmailCodeResult(
                saved.getId(),
                type
        );
    }

    // ============ 3) Verify second factor (phone / old email) AND commit ============

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

            // Domain layer validates second factor & moves to READY_TO_COMMIT
            request.verifySecondFactor(command.code(), now);

            if (request.getStatus() != EmailChangeStatus.READY_TO_COMMIT) {
                    throw new InvalidSecondFactorCodeException();
                }


            AuthUser user = authUserRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

            if (!user.getEmail().equals(request.getOldEmail())) {
                throw new EmailChangeEmailMismatchException();
            }

        // 3) Let domain model update its own state
        user.updateEmail(request.getNewEmail(), now);

        // 4) Persist updated user
        authUserRepositoryPort.save(user);

            // Mark request completed
            request.markCompleted(now);
            EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

            // Notify both old & new emails
            emailNotificationPort.notifyEmailChangedOldEmail(
                    request.getOldEmail(),
                    request.getNewEmail()
            );
            emailNotificationPort.notifyEmailChangedNewEmail(
                    request.getNewEmail()
            );

            // Optionally: revoke other sessions/refresh tokens inside userAccountPort

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

    private String generateCode() {
        int num = random.nextInt(1_000_000);
        return String.format("%06d", num);
    }
}