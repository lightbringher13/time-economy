package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;
import com.timeeconomy.auth.domain.verification.port.out.integration.VerificationOtpDeliveryPublisherPort;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.policy.VerificationOtpRateLimitPort;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateOtpService implements CreateOtpUseCase {

    private static final int OTP_LEN = 6;

    // ✅ Verification-owned policies
    private static final Duration DEFAULT_OTP_TTL = Duration.ofMinutes(10);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    // ✅ Big-co anti-spam defaults
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    private static final Duration SEND_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_SENDS_PER_WINDOW = 5;

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final VerificationOtpDeliveryPublisherPort otpDeliveryPublisherPort;
    private final VerificationOtpRateLimitPort rateLimitPort;
    private final java.time.Clock clock;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public CreateOtpResult createOtp(CreateOtpCommand command) {
        final Instant now = Instant.now(clock);

        // 0) validate
        if (command == null
                || command.subjectType() == null
                || isBlank(command.subjectId())
                || command.purpose() == null
                || command.channel() == null
                || isBlank(command.destination())) {
            return new CreateOtpResult(Outcome.INVALID_INPUT, null, null, "***", null);
        }

        // 1) normalize destination
        final String destNorm = normalizeDestination(command.channel(), command.destination());
        if (destNorm.isBlank()) {
            return new CreateOtpResult(Outcome.DESTINATION_INVALID, null, null, "***", null);
        }

        // 2) load pending ONCE (optional: to return challengeId/expiresAt in THROTTLED responses)
        Optional<VerificationChallenge> pendingOpt =
                repo.findActivePending(command.subjectType(), command.subjectId(), command.purpose(), command.channel());

        // keep pending status consistent (persist if expired)
        if (pendingOpt.isPresent()) {
            VerificationChallenge pending = pendingOpt.get();
            VerificationStatus before = pending.getStatus();
            pending.expireIfNeeded(now);
            if (pending.getStatus() != before) {
                repo.save(pending);
            }
        }

        // 3) rate-limit (cooldown + quota) — single decision point
        var policy = new VerificationOtpRateLimitPort.RateLimitPolicy(
                RESEND_COOLDOWN,
                SEND_WINDOW,
                MAX_SENDS_PER_WINDOW
        );

        var rl = rateLimitPort.tryConsume(
                new VerificationOtpRateLimitPort.RateLimitKey(command.purpose(), command.channel(), destNorm),
                policy,
                now
        );

        if (!rl.allowed()) {
            String pendingId = pendingOpt.filter(VerificationChallenge::isPending).map(VerificationChallenge::getId).orElse(null);
            Instant pendingExpires = pendingOpt.filter(VerificationChallenge::isPending).map(VerificationChallenge::getExpiresAt).orElse(null);

            // If limiter errored, you can either return INTERNAL_ERROR or conservatively THROTTLED.
            if (rl.reason() == VerificationOtpRateLimitPort.Reason.ERROR) {
                return new CreateOtpResult(
                        Outcome.INTERNAL_ERROR,
                        pendingId,
                        pendingExpires,
                        maskDestination(command.channel(), command.destination()),
                        rl.nextAllowedAt()
                );
            }

            return new CreateOtpResult(
                    Outcome.THROTTLED,
                    pendingId,
                    pendingExpires,
                    maskDestination(command.channel(), command.destination()),
                    rl.nextAllowedAt()
            );
        }

        // ✅ Verification decides security knobs
        final Duration ttl = DEFAULT_OTP_TTL;
        final int maxAttempts = DEFAULT_MAX_ATTEMPTS;

        // 4) cancel active pending (cancel + create model) using the loaded pending
        if (pendingOpt.isPresent() && pendingOpt.get().isPending()) {
            VerificationChallenge pending = pendingOpt.get();
            pending.cancel(now);
            repo.save(pending);
        }

        // 5) create fresh OTP challenge
        final String rawCode = generateOtp();
        final String codeHash = hasher.hash(rawCode);
        final Instant expiresAt = now.plus(ttl);

        VerificationChallenge challenge = VerificationChallenge.createOtpPending(
                command.purpose(),
                command.channel(),
                command.subjectType(),
                command.subjectId(),
                command.destination(),
                destNorm,
                codeHash,
                expiresAt,
                maxAttempts,
                now,
                command.requestIp(),
                command.userAgent()
        );

        VerificationChallenge saved = repo.save(challenge);

        // 6) store raw OTP for delivery/internal retrieval
        repo.putOtp(saved.getId(), rawCode, ttl);

        // 7) publish delivery request (async)
        int ttlSeconds = (int) Math.max(1, ttl.toSeconds());
        otpDeliveryPublisherPort.publish(new VerificationOtpDeliveryPublisherPort.PublishCommand(
                UUID.fromString(saved.getId()),
                command.purpose().name(),
                command.channel().name(),
                command.subjectType().name(),
                command.subjectId(),
                destNorm,
                ttlSeconds,
                now
        ));

        // nextSendAt = cooldown end from limiter (source of truth)
        return new CreateOtpResult(
                Outcome.SENT,
                saved.getId(),
                expiresAt,
                maskDestination(command.channel(), command.destination()),
                rl.nextAllowedAt()
        );
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LEN);
        int n = random.nextInt(bound);
        return String.format("%0" + OTP_LEN + "d", n);
    }

    private String normalizeDestination(VerificationChannel channel, String destination) {
        String d = destination == null ? "" : destination.trim();
        if (d.isBlank()) return "";
        if (channel == VerificationChannel.EMAIL) return d.toLowerCase();
        return d; // TODO: SMS E.164 normalize later
    }

    private String maskDestination(VerificationChannel channel, String destination) {
        if (destination == null) return "***";
        if (channel == VerificationChannel.EMAIL) {
            int at = destination.indexOf('@');
            if (at <= 1) return "***" + destination.substring(Math.max(0, at));
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        if (destination.length() <= 4) return "***";
        return destination.substring(0, 3) + "****" + destination.substring(destination.length() - 2);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}