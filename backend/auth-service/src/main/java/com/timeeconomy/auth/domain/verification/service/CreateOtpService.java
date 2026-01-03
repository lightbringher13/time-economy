package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.payload.VerificationOtpDeliveryRequestedPayload;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;

import java.util.UUID;
import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreateOtpService implements CreateOtpUseCase {

    private static final int OTP_LEN = 6;

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final OutboxPayloadSerializerPort outboxPayloadSerializerPort;
    private final java.time.Clock clock;

    private final SecureRandom random = new SecureRandom();

    // =========================
    // OTP
    // =========================

    @Override
    @Transactional
    public CreateOtpResult createOtp(CreateOtpCommand command) {
        Instant now = Instant.now(clock);

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
        Instant expiresAt = now.plus(command.ttl());

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
        
        repo.put(saved.getId(), rawCode, command.ttl());
        
        int ttlSeconds = (int) Math.max(1, command.ttl().toSeconds());

        String payloadJson = outboxPayloadSerializerPort.serialize(
                new VerificationOtpDeliveryRequestedPayload(
                        UUID.fromString(saved.getId()), // saved.getId() is String -> UUID
                        command.purpose().name(),
                        command.channel().name(),
                        command.subjectType().name(),
                        command.subjectId(),
                        normalizeDestination(command.channel(), command.destination()),
                        ttlSeconds
                )
        );

        OutboxEvent event = OutboxEvent.newPending(
                "verification_challenge",
                saved.getId(),
                "VerificationOtpDeliveryRequested.v1",
                payloadJson,
                now
        );

        outboxEventRepositoryPort.save(event);

        return new CreateOtpResult(
                saved.getId(),
                true,
                ttlMinutes,
                maskDestination(command.channel(), command.destination())
        );
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LEN);
        int n = random.nextInt(bound);
        return String.format("%0" + OTP_LEN + "d", n);
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