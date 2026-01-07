// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/changeemail/service/CancelEmailChangeService.java
package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeForbiddenException;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeInvalidStateException;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.port.in.CancelEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CancelEmailChangeService implements CancelEmailChangeUseCase {

    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort; // ✅ optional but recommended
    private final Clock clock;

    @Override
    @Transactional
    public void cancel(CancelCommand command) {
        Instant now = Instant.now(clock);

        EmailChangeRequest req = emailChangeRequestRepositoryPort.findById(command.requestId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.requestId()));

        if (!req.getUserId().equals(command.userId())) {
            throw new EmailChangeForbiddenException();
        }

        // expire & persist if needed
        if (req.expireIfNeeded(now)) {
            emailChangeRequestRepositoryPort.save(req);
            throw new EmailChangeInvalidStateException("cancel email change", req.getStatus());
        }

        // idempotent: if terminal -> just return
        if (req.getStatus() == EmailChangeStatus.COMPLETED
                || req.getStatus() == EmailChangeStatus.CANCELED
                || req.getStatus() == EmailChangeStatus.EXPIRED) {
            return;
        }

        // only allow cancel while active (your definition)
        if (!req.isActive()) {
            throw new EmailChangeInvalidStateException("cancel email change", req.getStatus());
        }

        req.markCanceled(now);
        emailChangeRequestRepositoryPort.save(req);

        // ✅ Optional but recommended: cancel pending OTP for new email verification
        verificationChallengeRepositoryPort.findActivePending(
                VerificationSubjectType.USER,
                command.userId().toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL
        ).ifPresent(ch -> {
            if (ch.getStatus() == VerificationStatus.PENDING) {
                ch.cancel(now);
                verificationChallengeRepositoryPort.save(ch);
            }
        });
    }
}