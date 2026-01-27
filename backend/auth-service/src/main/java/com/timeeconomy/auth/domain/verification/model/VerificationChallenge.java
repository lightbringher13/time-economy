package com.timeeconomy.auth.domain.verification.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class VerificationChallenge {

    private String id;

    private final VerificationPurpose purpose;
    private final VerificationChannel channel;

    private final VerificationSubjectType subjectType;
    private final String subjectId;

    private final String destination;       // raw (email/phone)
    private final String destinationNorm;   // normalized (lowercased email, e164 phone)

    // hashes only (never store raw code/token)
    private final String codeHash;    // OTP
    private final String tokenHash;   // LINK

    private VerificationStatus status;

    private final Instant expiresAt;
    private Instant verifiedAt;
    private Instant consumedAt;

    private int attemptCount;
    private final int maxAttempts;
    private int sentCount;
    private Instant lastSentAt;

    private final String requestIp;
    private final String userAgent;

    private final Instant createdAt;
    private Instant updatedAt;

    // ======================
    // Outcomes (Domain-first)
    // ======================

    public enum VerifyOutcome {
        VERIFIED,
        WRONG_CODE,        // includes wrong token
        EXPIRED,
        LOCKED,            // too many attempts
        NOT_PENDING,        // already verified/consumed/canceled/expired
        INVALID_INPUT
    }

    public enum ConsumeOutcome {
        CONSUMED,
        NOT_VERIFIED,
        NOT_PENDING
    }

    // ======================
    // Constructor
    // ======================

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
            VerificationStatus status,
            Instant expiresAt,
            Instant verifiedAt,
            Instant consumedAt,
            int attemptCount,
            int maxAttempts,
            int sentCount,
            Instant lastSentAt,
            String requestIp,
            String userAgent,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.subjectType = Objects.requireNonNull(subjectType, "subjectType");
        this.subjectId = requireText(subjectId, "subjectId");
        this.destination = requireText(destination, "destination");
        this.destinationNorm = requireText(destinationNorm, "destinationNorm");

        // ✅ invariant: exactly one of (codeHash, tokenHash)
        this.codeHash = blankToNull(codeHash);
        this.tokenHash = blankToNull(tokenHash);
        assertKindInvariant(this.codeHash, this.tokenHash);

        this.status = Objects.requireNonNull(status, "status");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.verifiedAt = verifiedAt;
        this.consumedAt = consumedAt;

        this.attemptCount = Math.max(0, attemptCount);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.sentCount = Math.max(0, sentCount);
        this.lastSentAt = Objects.requireNonNull(lastSentAt, "lastSentAt");

        this.requestIp = requestIp;
        this.userAgent = userAgent;

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");

        assertStateInvariant();
    }

    // ======================
    // Factories
    // ======================

    public static VerificationChallenge createOtpPending(
            VerificationPurpose purpose,
            VerificationChannel channel,
            VerificationSubjectType subjectType,
            String subjectId,
            String destination,
            String destinationNorm,
            String codeHash,
            Instant expiresAt,
            int maxAttempts,
            Instant now,
            String requestIp,
            String userAgent
    ) {
        requireText(codeHash, "codeHash");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(expiresAt, "expiresAt");

        return new VerificationChallenge(
                UUID.randomUUID().toString(),
                purpose,
                channel,
                subjectType,
                subjectId,
                destination,
                destinationNorm,
                codeHash,
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
            VerificationChannel channel,
            VerificationSubjectType subjectType,
            String subjectId,
            String destination,
            String destinationNorm,
            String tokenHash,
            Instant expiresAt,
            int maxAttempts,
            Instant now,
            String requestIp,
            String userAgent
    ) {
        requireText(tokenHash, "tokenHash");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(expiresAt, "expiresAt");

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
    // Helpers
    // ======================

    public boolean isOtp() { return codeHash != null; }
    public boolean isLink() { return tokenHash != null; }

    public boolean isPending() { return status == VerificationStatus.PENDING; }

    public boolean isExpired(Instant now) {
        return now != null && now.isAfter(expiresAt);
    }

    public void expireIfNeeded(Instant now) {
        if (status == VerificationStatus.PENDING && isExpired(now)) {
            status = VerificationStatus.EXPIRED;
            touch(now);
        }
    }

    public void cancel(Instant now) {
        // keep your rule (can’t cancel consumed)
        if (status == VerificationStatus.CONSUMED) {
            throw new IllegalStateException("Cannot cancel a CONSUMED challenge");
        }
        status = VerificationStatus.CANCELED;
        touch(now);
    }

    // ======================
    // ✅ 핵심 변경: verify는 outcome 반환 (no-throw path)
    // ======================

    public VerifyOutcome verifyOtpByHash(String providedCodeHash, Instant now) {
        if (!isOtp()) return VerifyOutcome.INVALID_INPUT;
        return verifyByHash(providedCodeHash, now, true);
    }

    public VerifyOutcome verifyLinkByHash(String providedTokenHash, Instant now) {
        if (!isLink()) return VerifyOutcome.INVALID_INPUT;
        return verifyByHash(providedTokenHash, now, false);
    }

    private VerifyOutcome verifyByHash(String providedHash, Instant now, boolean isOtp) {
        if (providedHash == null || providedHash.isBlank() || now == null) {
            return VerifyOutcome.INVALID_INPUT;
        }

        // 1) must be pending
        if (status != VerificationStatus.PENDING) {
            return VerifyOutcome.NOT_PENDING;
        }

        // 2) expiry check (domain owns it)
        if (isExpired(now)) {
            status = VerificationStatus.EXPIRED;
            touch(now);
            return VerifyOutcome.EXPIRED;
        }

        // 3) locked check (before attempt)
        if (attemptCount >= maxAttempts) {
            // ✅ keep your status model; lock => CANCELED (and outcome=LOCKED)
            status = VerificationStatus.CANCELED;
            touch(now);
            return VerifyOutcome.LOCKED;
        }

        // 4) consume an attempt
        attemptCount++;
        touch(now);

        final String expected = isOtp ? codeHash : tokenHash;
        boolean ok = Objects.equals(expected, providedHash);

        if (!ok) {
            // if this attempt caused exceed => lock now
            if (attemptCount >= maxAttempts) {
                status = VerificationStatus.CANCELED;
                touch(now);
                return VerifyOutcome.LOCKED;
            }
            return VerifyOutcome.WRONG_CODE;
        }

        // 5) success
        status = VerificationStatus.VERIFIED;
        verifiedAt = now;
        touch(now);
        return VerifyOutcome.VERIFIED;
    }

    // ======================
    // consume()
    // ======================

    public ConsumeOutcome consumeIfVerified(Instant now) {
        if (now == null) return ConsumeOutcome.NOT_PENDING;

        if (status == VerificationStatus.CONSUMED) return ConsumeOutcome.NOT_PENDING;
        if (status == VerificationStatus.VERIFIED) {
            status = VerificationStatus.CONSUMED;
            consumedAt = now;
            touch(now);
            return ConsumeOutcome.CONSUMED;
        }
        if (status == VerificationStatus.PENDING) return ConsumeOutcome.NOT_VERIFIED;
        return ConsumeOutcome.NOT_PENDING;
    }

    // ======================
    // send tracking
    // ======================

    public void recordSent(Instant now) {
        if (now == null) return;

        // big-co: usually only meaningful in PENDING, but safe no-op otherwise
        if (status != VerificationStatus.PENDING) return;

        sentCount++;
        lastSentAt = now;
        touch(now);
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    // ======================
    // Invariants
    // ======================

    private static void assertKindInvariant(String codeHash, String tokenHash) {
        boolean hasCode = codeHash != null;
        boolean hasToken = tokenHash != null;

        if (hasCode == hasToken) {
            // both true or both false => invalid
            throw new IllegalStateException("Challenge must be exactly one of OTP(codeHash) or LINK(tokenHash)");
        }
    }

    private void assertStateInvariant() {
        // VERIFIED/CONSUMED implies verifiedAt exists (optional but strongly recommended)
        if ((status == VerificationStatus.VERIFIED || status == VerificationStatus.CONSUMED) && verifiedAt == null) {
            // don’t throw if you have legacy data; but for new data this is good
            // throw new IllegalStateException("verifiedAt must be set when status=" + status);
        }

        // CONSUMED implies consumedAt exists (optional strictness)
        if (status == VerificationStatus.CONSUMED && consumedAt == null) {
            // same note as above
        }
    }

    private static String requireText(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " is required");
        return v;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
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

    public VerificationStatus getStatus() { return status; }

    public Instant getExpiresAt() { return expiresAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getConsumedAt() { return consumedAt; }

    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }

    public int getSentCount() { return sentCount; }
    public Instant getLastSentAt() { return lastSentAt; }

    public String getRequestIp() { return requestIp; }
    public String getUserAgent() { return userAgent; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}