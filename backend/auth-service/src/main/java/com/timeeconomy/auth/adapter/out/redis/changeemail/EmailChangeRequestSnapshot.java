package com.timeeconomy.auth.adapter.out.redis.changeemail;

import lombok.Builder;

@Builder
public record EmailChangeRequestSnapshot(
        int schemaVersion,

        Long id,
        Long userId,

        String oldEmail,
        String newEmail,

        String secondFactorType, // enum name
        String status,           // enum name

        Long expiresAtEpochMillis,
        Long createdAtEpochMillis,
        Long updatedAtEpochMillis,

        Long version
) {}