package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.common.notification.port.VerificationNotificationPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.port.in.CreateLinkUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CreateLinkService implements CreateLinkUseCase {

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final VerificationNotificationPort notifier;

    private final java.time.Clock clock;

    private final SecureRandom random = new SecureRandom();

    
    @Override
    @Transactional
    public CreateLinkResult createLink(CreateLinkCommand command) {
        Instant now = Instant.now(clock);

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
        Instant expiresAt = now.plus(command.ttl());

        // token can have its own TTL (often same as challenge TTL)
        Duration tokenTtl = (command.tokenTtl() != null) ? command.tokenTtl() : command.ttl();
        Instant tokenExpiresAt = now.plus(tokenTtl);

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