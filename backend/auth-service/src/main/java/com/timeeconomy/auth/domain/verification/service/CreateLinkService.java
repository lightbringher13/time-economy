package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.port.in.CreateLinkUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;
import com.timeeconomy.auth.domain.verification.port.out.integration.VerificationLinkDeliveryPublisherPort;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateLinkService implements CreateLinkUseCase {

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final VerificationLinkDeliveryPublisherPort linkDeliveryPublisherPort; // ✅ NEW
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

        VerificationChallenge challenge = VerificationChallenge.createLinkPending(
                command.purpose(),
                command.channel(),
                command.subjectType(),
                command.subjectId(),
                command.destination(),
                normalizeDestination(command.channel(), command.destination()),
                tokenHash,
                expiresAt,
                5,
                now,
                command.requestIp(),
                command.userAgent()
        );

        VerificationChallenge saved = repo.save(challenge);

        repo.putLinkToken(saved.getId(), rawToken, command.ttl());

        int ttlSeconds = (int) Math.max(1, command.ttl().toSeconds());

        // ✅ publish delivery request via port (adapter will write outbox)
        linkDeliveryPublisherPort.publish(new VerificationLinkDeliveryPublisherPort.PublishCommand(
                UUID.fromString(saved.getId()),
                command.purpose().name(),
                command.channel().name(),
                command.subjectType().name(),
                command.subjectId(),
                normalizeDestination(command.channel(), command.destination()),
                ttlSeconds,
                now
        ));

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

    private String normalizeDestination(VerificationChannel channel, String destination) {
        if (destination == null) return "";
        String d = destination.trim();
        if (channel == VerificationChannel.EMAIL) return d.toLowerCase();
        return d;
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
}