package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

import java.time.Instant;
import java.util.Optional;

public interface GetActiveEmailChangeUseCase {

    record GetActiveEmailChangeCommand(Long userId) {}

    record GetActiveEmailChangeResult(
            Long requestId,
            EmailChangeStatus status,
            SecondFactorType secondFactorType,
            String maskedNewEmail,
            Instant expiresAt
    ) {}

    Optional<GetActiveEmailChangeResult> getActive(GetActiveEmailChangeCommand command);
}