package com.timeeconomy.auth.domain.changeemail.model.payload;

import java.time.LocalDateTime;

public record EmailChangeCommittedPayload(
        String requestId,
        Long userId,
        String oldEmail,
        String newEmail,
        LocalDateTime occurredAt
) {}