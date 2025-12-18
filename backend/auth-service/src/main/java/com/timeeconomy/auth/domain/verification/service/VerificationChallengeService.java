package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.common.notification.port.VerificationNotificationPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.port.in.VerificationChallengeUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class VerificationChallengeService implements VerificationChallengeUseCase {

    private static final int OTP_LEN = 6;

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final VerificationNotificationPort notifier;
    private final java.time.Clock clock;

    private final SecureRandom random = new SecureRandom();

    // =========================
    // OTP
    // =========================

    @Override
    @Transactional
    public CreateOtpResult createOtp(CreateOtpCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) cancel active pending (unique index safe + prevent duplicate flows)
        repo.findActivePending(command.subjectType(), command.subjectId(), command.purpose(), command.channel())
                .ifPresent(pending -> {
                    pending.cancel(now);
                    repo.save(pending);
                });

        // 2) create new
        String rawCode = generateOtp();
        String codeHash = hasher.hash(rawCode);

        int ttlMinutes = (int) Math.max(1, command.ttl().toMinutes());
        LocalDateTime expiresAt = now.plus(command.ttl());

        VerificationChallenge challenge = VerificationChallenge.createOtpPending(
                command.purpose(),
                command.channel(),
                command.subjectType(),
                command.subjectId(),
                command.destination(),
                normalizeDestination(command.channel(), command.destination()),
                codeHash,
                expiresAt,
                command.maxAttempts(),
                now,
                command.requestIp(),
                command.userAgent()
        );

        VerificationChallenge saved = repo.save(challenge);

        // 3) notify
        notifier.sendOtp(command.channel(), command.destination(), command.purpose(), rawCode, ttlMinutes);

        return new CreateOtpResult(
                saved.getId(),
                true,
                ttlMinutes,
                maskDestination(command.channel(), command.destination())
        );
    }

    @Override
    @Transactional
    public VerifyOtpResult verifyOtp(VerifyOtpCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        var pendingOpt = repo.findActivePending(command.subjectType(), command.subjectId(), command.purpose(), command.channel());
        if (pendingOpt.isEmpty()) return new VerifyOtpResult(false);

        VerificationChallenge pending = pendingOpt.get();

        // bind destination
        String norm = normalizeDestination(command.channel(), command.destination());
        if (!pending.getDestinationNorm().equals(norm)) return new VerifyOtpResult(false);

        // expiry
        pending.markExpiredIfNeeded(now);
        if (pending.getStatus() != VerificationStatus.PENDING) {
            repo.save(pending);
            return new VerifyOtpResult(false);
        }

        // attempt limit
        if (pending.getAttemptCount() >= pending.getMaxAttempts()) {
            pending.cancel(now);
            repo.save(pending);
            return new VerifyOtpResult(false);
        }

        // verify
        String codeHash = hasher.hash(command.code());
        pending.recordAttempt(now);

        boolean ok = codeHash.equals(pending.getCodeHash());
        if (!ok) {
            repo.save(pending);
            return new VerifyOtpResult(false);
        }

        pending.markVerified(now);
        repo.save(pending);
        return new VerifyOtpResult(true);
    }

    // =========================
    // LINK
    // =========================

    @Override
    @Transactional
    public CreateLinkResult createLink(CreateLinkCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) cancel active pending for same subject+purpose+channel
        repo.findActivePending(command.subjectType(), command.subjectId(), command.purpose(), command.channel())
                .ifPresent(pending -> {
                    pending.cancel(now);
                    repo.save(pending);
                });

        // 2) generate token + hash
        String rawToken = generateLinkToken();
        String tokenHash = hasher.hash(rawToken);

        int ttlMinutes = (int) Math.max(1, command.ttl().toMinutes());
        LocalDateTime expiresAt = now.plus(command.ttl());

        // token can have its own TTL (often same as challenge TTL)
        LocalDateTime tokenExpiresAt = now.plus(
                command.tokenTtl() != null ? command.tokenTtl() : command.ttl()
        );

        VerificationChallenge challenge = VerificationChallenge.createLinkPending(
                command.purpose(),
                command.channel(),
                command.subjectType(),
                command.subjectId(),
                command.destination(),
                normalizeDestination(command.channel(), command.destination()),
                tokenHash,
                tokenExpiresAt,
                expiresAt,
                // link usually doesn’t need “max attempts”, but keep it consistent
                5,
                now,
                command.requestIp(),
                command.userAgent()
        );

        VerificationChallenge saved = repo.save(challenge);

        // 3) build URL + notify
        String linkUrl = buildLinkUrl(command.linkBaseUrl(), rawToken);

        notifier.sendLink(
                command.channel(),
                command.destination(),
                command.purpose(),
                linkUrl,
                ttlMinutes
        );

        return new CreateLinkResult(
                saved.getId(),
                true,
                ttlMinutes,
                maskDestination(command.channel(), command.destination())
        );
    }

    @Override
    @Transactional
    public VerifyLinkResult verifyLink(VerifyLinkCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        String tokenHash = hasher.hash(command.token());

        var pendingOpt = repo.findActivePendingByTokenHash(
                command.purpose(),
                command.channel(),
                tokenHash
        );
        if (pendingOpt.isEmpty()) return new VerifyLinkResult(false, null, null);

        VerificationChallenge pending = pendingOpt.get();

        pending.markExpiredIfNeeded(now);
        if (pending.getStatus() != VerificationStatus.PENDING) {
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        if (pending.getTokenExpiresAt() != null && now.isAfter(pending.getTokenExpiresAt())) {
            pending.cancel(now);
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        if (pending.getAttemptCount() >= pending.getMaxAttempts()) {
            pending.cancel(now);
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        pending.recordAttempt(now);

        boolean ok = tokenHash.equals(pending.getTokenHash());
        if (!ok) {
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        pending.markVerified(now);
        repo.save(pending);

        return new VerifyLinkResult(true, pending.getId(), pending.getDestinationNorm());
    }

    @Override
    @Transactional
    public void consume(ConsumeCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        VerificationChallenge ch = repo.findById(command.challengeId())
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + command.challengeId()));

        ch.consume(now);
        repo.save(ch);
    }

    // =========================
    // helpers
    // =========================

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LEN);
        int n = random.nextInt(bound);
        return String.format("%0" + OTP_LEN + "d", n);
    }

    private String generateLinkToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildLinkUrl(String baseUrl, String rawToken) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("linkBaseUrl is required");
        }
        String sep = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + sep + "token=" + rawToken;
    }

    private String normalizeDestination(VerificationChannel channel, String destination) {
        if (destination == null) return "";
        String d = destination.trim();
        if (channel == VerificationChannel.EMAIL) return d.toLowerCase();
        // TODO: SMS normalize to E.164 later
        return d;
    }

    private String maskDestination(VerificationChannel channel, String destination) {
        if (destination == null) return "***";
        if (channel == VerificationChannel.EMAIL) {
            int at = destination.indexOf('@');
            if (at <= 1) return "***" + destination.substring(Math.max(0, at));
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        // PHONE
        if (destination.length() <= 4) return "***";
        return destination.substring(0, 3) + "****" + destination.substring(destination.length() - 2);
    }
}