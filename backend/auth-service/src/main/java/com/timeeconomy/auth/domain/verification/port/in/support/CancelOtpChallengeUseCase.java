// auth/domain/verification/port/in/CancelOtpChallengeUseCase.java
package com.timeeconomy.auth.domain.verification.port.in.support;

import java.time.Instant;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface CancelOtpChallengeUseCase {

    enum Outcome {
        CANCELED,
        NOT_FOUND,
        NOT_PENDING,
        INVALID_INPUT
    }

    record Command(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            Instant now
    ) {}

    record Result(
            Outcome outcome,
            boolean canceled
    ) {}

    Result cancelPending(Command command);
}