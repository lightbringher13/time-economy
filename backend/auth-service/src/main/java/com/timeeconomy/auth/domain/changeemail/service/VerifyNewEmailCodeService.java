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
        Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidNewEmailCodeException();
        }

        // ✅ idempotent: already verified or beyond
        if (request.getStatus() == EmailChangeStatus.NEW_EMAIL_VERIFIED
                || request.getStatus() == EmailChangeStatus.SECOND_FACTOR_PENDING
                || request.getStatus() == EmailChangeStatus.READY_TO_COMMIT
                || request.getStatus() == EmailChangeStatus.COMPLETED) {
            return new VerifyNewEmailCodeResult(request.getId());
        }

        if (request.getStatus() != EmailChangeStatus.PENDING) {
            throw new InvalidNewEmailCodeException();
        }

        var verify = verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
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

        request.markNewEmailVerified(now);
        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        return new VerifyNewEmailCodeResult(saved.getId());
    }
}