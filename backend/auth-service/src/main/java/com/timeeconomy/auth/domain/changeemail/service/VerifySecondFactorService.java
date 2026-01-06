package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeStateCorruptedException;
import com.timeeconomy.auth.domain.changeemail.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifySecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VerifySecondFactorService implements VerifySecondFactorUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public VerifySecondFactorResult verifySecondFactor(VerifySecondFactorCommand command) {
        final Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        // 1) expired → persist EXPIRED best-effort then fail
        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidSecondFactorCodeException(); // later: InvalidEmailChangeRequestException
        }

        // 2) terminal states → fail fast
        if (request.getStatus() == EmailChangeStatus.CANCELED
                || request.getStatus() == EmailChangeStatus.EXPIRED) {
            throw new InvalidSecondFactorCodeException();
        }

        // 3) idempotent (but still enforce invariants)
        if (request.getStatus() == EmailChangeStatus.READY_TO_COMMIT
                || request.getStatus() == EmailChangeStatus.COMPLETED) {

            if (request.getSecondFactorType() == null) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        request.getStatus() + " but secondFactorType is null"
                );
            }

            // optional strict invariants for "done" states
            if (request.getStatus() == EmailChangeStatus.COMPLETED) {
                String newEmail = request.getNewEmail();
                if (newEmail == null || newEmail.isBlank()) {
                    throw new EmailChangeStateCorruptedException(
                            request.getId(),
                            request.getUserId(),
                            "COMPLETED but newEmail is missing"
                    );
                }
            }

            return new VerifySecondFactorResult(request.getId(), request.getStatus());
        }

        // 4) only allowed transition
        if (request.getStatus() != EmailChangeStatus.SECOND_FACTOR_PENDING) {
            throw new InvalidSecondFactorCodeException();
        }

        // 5) invariants for SECOND_FACTOR_PENDING
        final SecondFactorType type = request.getSecondFactorType();
        if (type == null) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    "SECOND_FACTOR_PENDING but secondFactorType is null"
            );
        }

        // 6) verify OTP depending on type
        final boolean ok = verifyBySecondFactorType(type, command, request);
        if (!ok) {
            throw new InvalidSecondFactorCodeException();
        }

        // 7) transition guard
        EmailChangeStatus before = request.getStatus();
        request.markReadyToCommit(now);

        if (before == request.getStatus()) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    "markReadyToCommit did not transition state"
            );
        }

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        // optional strict assert (matches your StartSecondFactor style)
        if (saved.getStatus() != EmailChangeStatus.READY_TO_COMMIT) {
            throw new EmailChangeStateCorruptedException(
                    saved.getId(),
                    saved.getUserId(),
                    "verifySecondFactor saved but status is not READY_TO_COMMIT"
            );
        }

        return new VerifySecondFactorResult(saved.getId(), saved.getStatus());
    }

    private boolean verifyBySecondFactorType(
            SecondFactorType type,
            VerifySecondFactorCommand command,
            EmailChangeRequest request
    ) {
        if (type == SecondFactorType.PHONE) {
            AuthUser user = authUserRepositoryPort.findById(command.userId())
                    .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

            String phone = user.getPhoneNumber();
            if (phone == null || phone.isBlank()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "secondFactorType=PHONE but user.phoneNumber is missing"
                );
            }

            return verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
                    VerificationSubjectType.USER,
                    command.userId().toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_PHONE,
                    VerificationChannel.SMS,
                    phone,
                    command.code()
            )).success();
        }

        // OLD_EMAIL
        String oldEmail = request.getOldEmail();
        if (oldEmail == null || oldEmail.isBlank()) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    "secondFactorType=OLD_EMAIL but request.oldEmail is missing"
            );
        }

        return verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.USER,
                command.userId().toString(),
                VerificationPurpose.CHANGE_EMAIL_2FA_OLD_EMAIL,
                VerificationChannel.EMAIL,
                oldEmail,
                command.code()
        )).success();
    }
}