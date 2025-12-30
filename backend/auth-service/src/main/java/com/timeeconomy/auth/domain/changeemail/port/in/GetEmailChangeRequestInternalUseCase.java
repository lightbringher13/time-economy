package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

import java.time.Instant;

public interface GetEmailChangeRequestInternalUseCase {

    Result getById(Long requestId);

    record Result(
            Long requestId,
            Long userId,
            String oldEmail,
            String newEmail,
            EmailChangeStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}