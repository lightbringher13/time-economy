// auth/domain/verification/service/CancelOtpChallengeService.java
package com.timeeconomy.auth.domain.verification.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.port.in.support.CancelOtpChallengeUseCase;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

@Service
@RequiredArgsConstructor
public class CancelOtpChallengeService implements CancelOtpChallengeUseCase {

    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort;

    @Override
    @Transactional
    public Result cancelPending(Command c) {
        if (c == null
                || c.subjectType() == null
                || c.purpose() == null
                || c.channel() == null
                || c.now() == null
                || c.subjectId() == null
                || c.subjectId().isBlank()) {
            return new Result(Outcome.INVALID_INPUT, false);
        }

        var opt = verificationChallengeRepositoryPort.findActivePending(
                c.subjectType(),
                c.subjectId(),
                c.purpose(),
                c.channel()
        );

        if (opt.isEmpty()) {
            return new Result(Outcome.NOT_FOUND, false);
        }

        var ch = opt.get();
        if (ch.getStatus() != VerificationStatus.PENDING) {
            return new Result(Outcome.NOT_PENDING, false);
        }

        ch.cancel(c.now());
        verificationChallengeRepositoryPort.save(ch);

        return new Result(Outcome.CANCELED, true);
    }
}