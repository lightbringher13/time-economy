package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

import java.time.Instant;

public interface GetEmailChangeStatusUseCase {

    record GetEmailChangeStatusCommand(
            Long userId,
            Long requestId
    ) {}

    record GetEmailChangeStatusResult(
            Long requestId,
            EmailChangeStatus status,
            SecondFactorType secondFactorType,  // nullable unless SECOND_FACTOR_PENDING+
            String maskedNewEmail,              // nullable if missing
            Instant expiresAt
    ) {}

    GetEmailChangeStatusResult getStatus(GetEmailChangeStatusCommand command);
}