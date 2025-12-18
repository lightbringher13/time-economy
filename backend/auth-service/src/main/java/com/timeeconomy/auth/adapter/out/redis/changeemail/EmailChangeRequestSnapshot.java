package com.timeeconomy.auth.adapter.out.redis.changeemail;

import lombok.Builder;

@Builder
public record EmailChangeRequestSnapshot(
        int schemaVersion,

        String id,
        String userId,

        String oldEmail,
        String newEmail,

        String secondFactorType,
        String status,

        String expiresAt,
        String createdAt,
        String updatedAt,

        String version
) {}