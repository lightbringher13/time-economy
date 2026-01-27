package com.timeeconomy.auth.adapter.out.verification.policy;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.out.policy.VerificationOtpRateLimitPort;

@Component
@RequiredArgsConstructor
public class RedisVerificationOtpRateLimitAdapter implements VerificationOtpRateLimitPort {

    private final StringRedisTemplate redis;

    @Override
    public RateLimitResult tryConsume(RateLimitKey key, RateLimitPolicy policy, Instant now) {
        final Instant t = (now != null) ? now : Instant.now();

        try {
            if (key == null || policy == null
                    || key.purpose() == null || key.channel() == null
                    || isBlank(key.destinationNorm())
                    || policy.cooldown() == null || policy.window() == null
                    || policy.cooldown().isNegative() || policy.window().isNegative()
                    || policy.maxPerWindow() <= 0) {
                return new RateLimitResult(false, Reason.INVALID_INPUT, t, 0);
            }

            final String cooldownKey = cooldownKey(key.purpose(), key.channel(), key.destinationNorm());
            final String quotaKey = quotaKey(key.purpose(), key.channel(), key.destinationNorm());

            // 1) COOLDOWN gate (do NOT consume quota if blocked)
            Boolean cooldownOk = redis.opsForValue().setIfAbsent(cooldownKey, "1", policy.cooldown());
            if (Boolean.FALSE.equals(cooldownOk)) {
                Instant nextByCooldown = nextAllowedFromTtlSeconds(cooldownKey, t);

                // Optional: if quota already exceeded, return the later time
                Instant nextByQuota = null;
                int quotaCount = readInt(quotaKey, 0);
                if (quotaCount > policy.maxPerWindow()) {
                    nextByQuota = nextAllowedFromTtlSeconds(quotaKey, t);
                }

                Instant next = later(nextByCooldown, nextByQuota);
                return new RateLimitResult(false, Reason.COOLDOWN, next, quotaCount);
            }

            // 2) QUOTA window (consume 1 send)
            Long count = redis.opsForValue().increment(quotaKey);
            if (count != null && count == 1L) {
                redis.expire(quotaKey, policy.window());
            }

            int c = (count == null) ? 0 : count.intValue();
            if (c > policy.maxPerWindow()) {
                Instant next = nextAllowedFromTtlSeconds(quotaKey, t);
                return new RateLimitResult(false, Reason.QUOTA_EXCEEDED, next, c);
            }

            // allowed: next resend time is cooldown end
            return new RateLimitResult(true, Reason.ALLOWED, t.plus(policy.cooldown()), c);

        } catch (Exception e) {
            // fail-safe: block if limiter has issues
            return new RateLimitResult(false, Reason.ERROR, t.plusSeconds(1), 0);
        }
    }

    private Instant nextAllowedFromTtlSeconds(String key, Instant now) {
        Long ttlSec = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttlSec == null || ttlSec <= 0) return now.plusSeconds(1);
        return now.plusSeconds(ttlSec);
    }

    private int readInt(String key, int def) {
        try {
            String v = redis.opsForValue().get(key);
            if (v == null || v.isBlank()) return def;
            return Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }

    private Instant later(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private String cooldownKey(VerificationPurpose purpose, VerificationChannel channel, String destNorm) {
        return "verif:otp:cd:" + purpose.name() + ":" + channel.name() + ":" + destNorm;
    }

    private String quotaKey(VerificationPurpose purpose, VerificationChannel channel, String destNorm) {
        return "verif:otp:q:" + purpose.name() + ":" + channel.name() + ":" + destNorm;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}