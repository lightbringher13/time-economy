// domain/verification/port/out/policy/VerificationOtpRateLimitPort.java
package com.timeeconomy.auth.domain.verification.port.out.policy;

import java.time.Duration;
import java.time.Instant;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface VerificationOtpRateLimitPort {

    RateLimitResult tryConsume(RateLimitKey key, RateLimitPolicy policy, Instant now);

    record RateLimitKey(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String destinationNorm
    ) {}

    record RateLimitPolicy(
            Duration cooldown,        // e.g. 30s
            Duration window,          // e.g. 10m
            int maxPerWindow          // e.g. 5
    ) {}

    enum Reason {
        ALLOWED,
        COOLDOWN,
        QUOTA_EXCEEDED,
        INVALID_INPUT,
        ERROR
    }

    record RateLimitResult(
            boolean allowed,
            Reason reason,
            Instant nextAllowedAt,    // if blocked: retry time; if allowed: earliest resend time (= now + cooldown)
            int currentCount          // best-effort quota count in window (0 if unknown)
    ) {}
}