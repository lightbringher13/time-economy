package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.policy.VerificationOtpVerifyRateLimitPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VerifyOtpService implements VerifyOtpUseCase {

    // ✅ big-co defaults (tune later)
    private static final VerificationOtpVerifyRateLimitPort.RateLimitPolicy VERIFY_LIMIT =
            new VerificationOtpVerifyRateLimitPort.RateLimitPolicy(
                    Duration.ofSeconds(1),     // cooldown
                    Duration.ofMinutes(10),    // window
                    30                         // max attempts per window
            );

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final VerificationOtpVerifyRateLimitPort verifyRateLimitPort; // ✅ ADD
    private final Clock clock;

    @Override
    @Transactional
    public VerifyOtpResult verifyOtp(VerifyOtpCommand command) {
        final Instant now = Instant.now(clock);

        // 0) validate
        if (command == null
                || command.subjectType() == null
                || isBlank(command.subjectId())
                || command.purpose() == null
                || command.channel() == null
                || isBlank(command.destination())
                || isBlank(command.code())) {
            return new VerifyOtpResult(Outcome.INVALID_INPUT, null, null, null);
        }

        final String destNorm = normalizeDestination(command.channel(), command.destination());
        if (isBlank(destNorm)) {
            return new VerifyOtpResult(Outcome.INVALID_INPUT, null, null, null);
        }

        final String ip = safeIp(command.requestIp());

        // 1) rate-limit verify attempts (cooldown + quota)
        var rl = verifyRateLimitPort.tryConsume(
                new VerificationOtpVerifyRateLimitPort.RateLimitKey(
                        command.purpose(),
                        command.channel(),
                        destNorm,
                        ip
                ),
                VERIFY_LIMIT,
                now
        );

        if (rl == null || !rl.allowed()) {
            // Optional: return pending info to help UX (not required).
            // Big-co security default is to NOT leak whether a challenge exists.
            Integer retrySec = toRetryAfterSeconds(now, (rl == null ? null : rl.nextAllowedAt()));
            return new VerifyOtpResult(Outcome.THROTTLED, null, null, retrySec);
        }

        // 2) find active pending (subject+purpose+channel)
        var pendingOpt = repo.findActivePending(
                command.subjectType(),
                command.subjectId(),
                command.purpose(),
                command.channel()
        );

        if (pendingOpt.isEmpty()) {
            return new VerifyOtpResult(Outcome.NO_PENDING_CHALLENGE, null, null, null);
        }

        VerificationChallenge pending = pendingOpt.get();

        // keep expiry consistent
        pending.expireIfNeeded(now);
        if (!pending.isPending()) {
            repo.save(pending);
            return new VerifyOtpResult(Outcome.NO_PENDING_CHALLENGE, null, null, null);
        }

        // 3) destination bind
        if (!destNorm.equals(pending.getDestinationNorm())) {
            return new VerifyOtpResult(
                    Outcome.DESTINATION_MISMATCH,
                    pending.getExpiresAt(),
                    remainingAttempts(pending),
                    null
            );
        }

        // 4) verify using domain outcome (no throw)
        String providedHash = hasher.hash(command.code());
        VerificationChallenge.VerifyOutcome vo = pending.verifyOtpByHash(providedHash, now);

        repo.save(pending);

        Outcome out = switch (vo) {
            case VERIFIED -> Outcome.VERIFIED;
            case WRONG_CODE -> Outcome.WRONG_CODE;
            case EXPIRED -> Outcome.EXPIRED;
            case LOCKED -> Outcome.LOCKED;
            case NOT_PENDING -> Outcome.NO_PENDING_CHALLENGE;
            case INVALID_INPUT -> Outcome.INVALID_INPUT;
        };

        return new VerifyOtpResult(
                out,
                pending.getExpiresAt(),
                remainingAttempts(pending),
                null
        );
    }

    private Integer remainingAttempts(VerificationChallenge c) {
        if (c == null) return null;
        return Math.max(0, c.getMaxAttempts() - c.getAttemptCount());
    }

    private String normalizeDestination(VerificationChannel channel, String destination) {
        if (destination == null) return "";
        String d = destination.trim();
        if (d.isBlank()) return "";
        if (channel == VerificationChannel.EMAIL) return d.toLowerCase();
        return d; // TODO: E.164 later
    }

    private String safeIp(String ip) {
        // if client IP missing, still apply limiter under a shared bucket
        return isBlank(ip) ? "-" : ip.trim();
    }

    private Integer toRetryAfterSeconds(Instant now, Instant nextAllowedAt) {
        if (now == null || nextAllowedAt == null) return null;
        long sec = Duration.between(now, nextAllowedAt).getSeconds();
        return (int) Math.max(0, sec);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}