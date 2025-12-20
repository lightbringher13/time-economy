package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

import java.time.LocalDateTime;

public interface GetEmailChangeRequestInternalUseCase {

    Result getById(Long requestId);

    record Result(
            Long requestId,
            Long userId,
            String oldEmail,
            String newEmail,
            EmailChangeStatus status,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}