package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifyNewEmailCodeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.exception.InvalidNewEmailCodeException;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeStateCorruptedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class VerifyNewEmailCodeService implements VerifyNewEmailCodeUseCase {

    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public VerifyNewEmailCodeResult verifyNewEmailCode(VerifyNewEmailCodeCommand command) {
        final Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        // 1) expired → persist EXPIRED best-effort then fail
        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidNewEmailCodeException();
        }

        // 2) non-active terminal states → fail fast
        if (request.getStatus() == EmailChangeStatus.CANCELED
                || request.getStatus() == EmailChangeStatus.EXPIRED) {
            throw new InvalidNewEmailCodeException();
        }

        // 3) idempotent: already verified or beyond
        if (request.getStatus() == EmailChangeStatus.NEW_EMAIL_VERIFIED
                || request.getStatus() == EmailChangeStatus.SECOND_FACTOR_PENDING
                || request.getStatus() == EmailChangeStatus.READY_TO_COMMIT
                || request.getStatus() == EmailChangeStatus.COMPLETED) {
            return new VerifyNewEmailCodeResult(request.getId(), request.getStatus());
        }

        // 4) only allowed transition
        if (request.getStatus() != EmailChangeStatus.PENDING) {
            throw new InvalidNewEmailCodeException();
        }

        // 5) invariants for verification
        String newEmail = request.getNewEmail();
        if (newEmail == null || newEmail.isBlank()) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    "PENDING but newEmail is missing"
            );
        }

        boolean ok = verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.USER,
                command.userId().toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL,
                newEmail,
                command.code()
        )).success();

        if (!ok) {
            throw new InvalidNewEmailCodeException();
        }

        // 6) guard against silent no-op
        EmailChangeStatus before = request.getStatus();
        request.markNewEmailVerified(now);

        if (before == request.getStatus()) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    "markNewEmailVerified did not transition state"
            );
        }

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        // optional strict assert (matches your StartSecondFactor style)
        if (saved.getStatus() != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
            throw new EmailChangeStateCorruptedException(
                    saved.getId(),
                    saved.getUserId(),
                    "verifyNewEmailCode saved but status is not NEW_EMAIL_VERIFIED"
            );
        }

        return new VerifyNewEmailCodeResult(saved.getId(), saved.getStatus());
    }
}