// domain/verification/port/out/policy/VerificationOtpVerifyRateLimitPort.java
package com.timeeconomy.auth.domain.verification.port.out.policy;

import java.time.Instant;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface VerificationOtpVerifyRateLimitPort {

    RateLimitResult tryConsume(RateLimitKey key, RateLimitPolicy policy, Instant now);

    record RateLimitKey(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String destinationNorm,
            String clientIp // optional but strongly recommended
    ) {}

    record RateLimitPolicy(
            java.time.Duration cooldown, // e.g. 1s
            java.time.Duration window,   // e.g. 10m
            int maxPerWindow             // e.g. 30
    ) {}

    enum Reason { ALLOWED, COOLDOWN, QUOTA_EXCEEDED, INVALID_INPUT, ERROR }

    record RateLimitResult(
            boolean allowed,
            Reason reason,
            Instant nextAllowedAt,
            int currentCount
    ) {
        public Integer retryAfterSeconds(Instant now) {
            if (allowed || now == null || nextAllowedAt == null) return null;
            long sec = java.time.Duration.between(now, nextAllowedAt).getSeconds();
            return (int) Math.max(0, sec);
        }
    }
}