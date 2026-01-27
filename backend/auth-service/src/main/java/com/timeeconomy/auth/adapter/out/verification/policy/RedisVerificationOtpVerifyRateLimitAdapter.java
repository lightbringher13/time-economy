// src/main/java/com/timeeconomy/auth/adapter/out/verification/policy/RedisVerificationOtpVerifyRateLimitAdapter.java
package com.timeeconomy.auth.adapter.out.verification.policy;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.out.policy.VerificationOtpVerifyRateLimitPort;

@Component
@RequiredArgsConstructor
public class RedisVerificationOtpVerifyRateLimitAdapter implements VerificationOtpVerifyRateLimitPort {

    private final StringRedisTemplate redis;

    @Override
    public RateLimitResult tryConsume(RateLimitKey key, RateLimitPolicy policy, Instant now) {
        // ✅ fail-safe: if inputs are invalid, block (conservative)
        if (key == null || policy == null || now == null
                || key.purpose() == null || key.channel() == null
                || isBlank(key.destinationNorm())
                || isBlank(key.clientIp())
                || policy.cooldown() == null || policy.window() == null
                || policy.cooldown().isNegative() || policy.window().isNegative()
                || policy.maxPerWindow() <= 0) {
            Instant next = (now == null) ? Instant.now().plusSeconds(1) : now.plusSeconds(1);
            return new RateLimitResult(false, Reason.INVALID_INPUT, next, 0);
        }

        final String cdKey = cooldownKey(key.purpose(), key.channel(), key.destinationNorm(), key.clientIp());
        final String qKey  = quotaKey(key.purpose(), key.channel(), key.destinationNorm(), key.clientIp());

        try {
            // -------------------------
            // 1) COOLDOWN gate (do NOT consume quota if blocked)
            // -------------------------
            Boolean cooldownOk = redis.opsForValue().setIfAbsent(cdKey, "1", policy.cooldown());
            if (Boolean.FALSE.equals(cooldownOk)) {
                Instant nextByCooldown = nextAllowedFromTtlSeconds(cdKey, now);

                // (optional) if quota already exceeded, return later(nextCooldown, nextQuota)
                int qCount = readInt(qKey, 0);
                Instant nextByQuota = (qCount > policy.maxPerWindow())
                        ? nextAllowedFromTtlSeconds(qKey, now)
                        : null;

                Instant next = later(nextByCooldown, nextByQuota);
                return new RateLimitResult(false, Reason.COOLDOWN, next, qCount);
            }

            // -------------------------
            // 2) QUOTA window (consume 1 verification attempt)
            // -------------------------
            Long count = redis.opsForValue().increment(qKey);
            if (count != null && count == 1L) {
                // first hit starts the window
                redis.expire(qKey, policy.window());
            }

            int c = (count == null) ? 0 : count.intValue();

            if (c > policy.maxPerWindow()) {
                Instant next = nextAllowedFromTtlSeconds(qKey, now);
                return new RateLimitResult(false, Reason.QUOTA_EXCEEDED, next, c);
            }

            // allowed: next attempt earliest is cooldown end
            return new RateLimitResult(true, Reason.ALLOWED, now.plus(policy.cooldown()), c);

        } catch (Exception e) {
            // ✅ big-co conservative default: block on limiter failure (prevents abuse if Redis is flaky)
            return new RateLimitResult(false, Reason.ERROR, now.plusSeconds(1), 0);
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

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

    private String cooldownKey(VerificationPurpose purpose, VerificationChannel channel, String destNorm, String ip) {
        return "verif:otp:verify:cd:" + purpose.name() + ":" + channel.name() + ":" + destNorm + ":" + ip;
    }

    private String quotaKey(VerificationPurpose purpose, VerificationChannel channel, String destNorm, String ip) {
        return "verif:otp:verify:q:" + purpose.name() + ":" + channel.name() + ":" + destNorm + ":" + ip;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}