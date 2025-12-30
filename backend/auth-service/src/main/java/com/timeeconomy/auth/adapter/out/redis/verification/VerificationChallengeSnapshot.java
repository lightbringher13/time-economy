package com.timeeconomy.auth.adapter.out.redis.verification;

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

        Long tokenExpiresAtEpochMillis,

        String status,

        Long expiresAtEpochMillis,
        Long verifiedAtEpochMillis,
        Long consumedAtEpochMillis,

        int attemptCount,
        int maxAttempts,
        int sentCount,
        Long lastSentAtEpochMillis,

        String requestIp,
        String userAgent,

        Long createdAtEpochMillis,
        Long updatedAtEpochMillis
) {}