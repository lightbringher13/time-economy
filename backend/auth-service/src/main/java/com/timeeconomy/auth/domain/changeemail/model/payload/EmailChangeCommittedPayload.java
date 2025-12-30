package com.timeeconomy.auth.domain.changeemail.model.payload;

import java.time.Instant;

public record EmailChangeCommittedPayload(
        String requestId,
        Long userId,
        String oldEmail,
        String newEmail,
        Instant occurredAt
) {}