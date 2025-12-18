package com.timeeconomy.auth_service.domain.verification.model;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

public class VerificationChallenge {

    private String id;

    private final VerificationPurpose purpose;
    private final VerificationChannel channel;

    private final VerificationSubjectType subjectType;
    private final String subjectId;

    private final String destination;       // raw (email/phone)
    private final String destinationNorm;   // normalized (lowercased email, e164 phone)

    // hashes only (never store raw code/token)
    private String codeHash;    // OTP
    private String tokenHash;   // link token
    private LocalDateTime tokenExpiresAt;

    private VerificationStatus status;

    private final LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime consumedAt;

    private int attemptCount;
    private int maxAttempts;
    private int sentCount;
    private LocalDateTime lastSentAt;

    private String requestIp;
    private String userAgent;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VerificationChallenge(
            String id,
            VerificationPurpose purpose,
            VerificationChannel channel,
            VerificationSubjectType subjectType,
            String subjectId,
            String destination,
            String destinationNorm,
            String codeHash,
            String tokenHash,
            LocalDateTime tokenExpiresAt,
            VerificationStatus status,
            LocalDateTime expiresAt,
            LocalDateTime verifiedAt,
            LocalDateTime consumedAt,
            int attemptCount,
            int maxAttempts,
            int sentCount,
            LocalDateTime lastSentAt,
            String requestIp,
            String userAgent,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.subjectType = Objects.requireNonNull(subjectType, "subjectType");
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.destinationNorm = Objects.requireNonNull(destinationNorm, "destinationNorm");

        this.codeHash = codeHash;
        this.tokenHash = tokenHash;
        this.tokenExpiresAt = tokenExpiresAt;

        this.status = Objects.requireNonNull(status, "status");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.verifiedAt = verifiedAt;
        this.consumedAt = consumedAt;

        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.sentCount = sentCount;
        this.lastSentAt = Objects.requireNonNull(lastSentAt, "lastSentAt");

        this.requestIp = requestIp;
        this.userAgent = userAgent;

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    // ======================
    // Factories (recommended)
    // ======================

    public static VerificationChallenge createOtpPending(
            VerificationPurpose purpose,
            VerificationChannel channel,                 // EMAIL/SMS
            VerificationSubjectType subjectType,
            String subjectId,
            String destination,
            String destinationNorm,
            String codeHash,
            LocalDateTime expiresAt,
            int maxAttempts,
            LocalDateTime now,
            String requestIp,
            String userAgent
    ) {
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("codeHash is required for OTP challenge");
        }

        return new VerificationChallenge(
                null,
                purpose,
                channel,
                subjectType,
                subjectId,
                destination,
                destinationNorm,
                codeHash,
                null,
                null,
                VerificationStatus.PENDING,
                expiresAt,
                null,
                null,
                0,
                maxAttempts,
                1,
                now,
                requestIp,
                userAgent,
                now,
                now
        );
    }

    public static VerificationChallenge createLinkPending(
            VerificationPurpose purpose,
            VerificationChannel channel,                 // usually EMAIL
            VerificationSubjectType subjectType,
            String subjectId,
            String destination,
            String destinationNorm,
            String tokenHash,
            LocalDateTime tokenExpiresAt,
            LocalDateTime expiresAt,
            int maxAttempts,
            LocalDateTime now,
            String requestIp,
            String userAgent
    ) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash is required for LINK challenge");
        }

        return new VerificationChallenge(
                UUID.randomUUID().toString(),
                purpose,
                channel,
                subjectType,
                subjectId,
                destination,
                destinationNorm,
                null,
                tokenHash,
                tokenExpiresAt,
                VerificationStatus.PENDING,
                expiresAt,
                null,
                null,
                0,
                maxAttempts,
                1,
                now,
                requestIp,
                userAgent,
                now,
                now
        );
    }

    // ======================
    // Domain behaviors
    // ======================

    public boolean isPending() {
        return status == VerificationStatus.PENDING;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public void expireIfNeeded(LocalDateTime now) {
        if (status == VerificationStatus.PENDING && isExpired(now)) {
            status = VerificationStatus.EXPIRED;
            updatedAt = now;
        }
    }

    public void markExpiredIfNeeded(LocalDateTime now) {
        if (status != VerificationStatus.PENDING) {
            return; // 이미 끝난 챌린지는 건드리지 않음
        }

        if (now.isAfter(expiresAt)) {
            status = VerificationStatus.EXPIRED;
            updatedAt = now;
        }
    }

    public void cancel(LocalDateTime now) {
        if (status == VerificationStatus.CONSUMED) {
            throw new IllegalStateException("Cannot cancel a CONSUMED challenge");
        }
        status = VerificationStatus.CANCELED;
        updatedAt = now;
    }

    public void recordAttempt(LocalDateTime now) {
        if (status != VerificationStatus.PENDING) return;
        attemptCount++;
        updatedAt = now;
    }

    public boolean isAttemptsExceeded() {
        return attemptCount >= maxAttempts;
    }

    public void markVerified(LocalDateTime now) {
        if (status != VerificationStatus.PENDING) {
            throw new IllegalStateException("Cannot verify when status=" + status);
        }
        if (isExpired(now)) {
            status = VerificationStatus.EXPIRED;
            updatedAt = now;
            throw new IllegalStateException("Challenge expired");
        }
        if (isAttemptsExceeded()) {
            status = VerificationStatus.CANCELED;
            updatedAt = now;
            throw new IllegalStateException("Too many attempts");
        }
        status = VerificationStatus.VERIFIED;
        verifiedAt = now;
        updatedAt = now;
    }

    public void consume(LocalDateTime now) {
        if (status != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Cannot consume when status=" + status);
        }
        status = VerificationStatus.CONSUMED;
        consumedAt = now;
        updatedAt = now;
    }

    public void recordSent(LocalDateTime now) {
        sentCount++;
        lastSentAt = now;
        updatedAt = now;
    }

    // ======================
    // Getters / minimal setters
    // ======================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public VerificationPurpose getPurpose() { return purpose; }
    public VerificationChannel getChannel() { return channel; }
    public VerificationSubjectType getSubjectType() { return subjectType; }
    public String getSubjectId() { return subjectId; }

    public String getDestination() { return destination; }
    public String getDestinationNorm() { return destinationNorm; }

    public String getCodeHash() { return codeHash; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }

    public VerificationStatus getStatus() { return status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }

    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }

    public int getSentCount() { return sentCount; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }

    public String getRequestIp() { return requestIp; }
    public String getUserAgent() { return userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}