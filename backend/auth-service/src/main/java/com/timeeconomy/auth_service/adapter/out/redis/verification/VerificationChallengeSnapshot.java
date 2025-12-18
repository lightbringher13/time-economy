package com.timeeconomy.auth_service.adapter.out.redis.verification;

import lombok.Builder;

@Builder
public record VerificationChallengeSnapshot(
        int schemaVersion,
        String id,

        String purpose,
        String channel,
        String subjectType,
        String subjectId,

        String destination,
        String destinationNorm,

        String codeHash,
        String tokenHash,
        String tokenExpiresAt,

        String status,

        String expiresAt,
        String verifiedAt,
        String consumedAt,

        String attemptCount,
        String maxAttempts,
        String sentCount,
        String lastSentAt,

        String requestIp,
        String userAgent,

        String createdAt,
        String updatedAt
) {}