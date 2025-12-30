package com.timeeconomy.auth.domain.auth.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain Model for authentication sessions / refresh tokens.
 * Pure domain: no JPA, no Spring.
 *
 * Uses Instant (UTC) to match TIMESTAMPTZ in DB.
 */
public class AuthSession {

    private Long id;                   // DB primary key
    private Long userId;               // Provided by user-service
    private String familyId;           // Identifies the device family
    private String tokenHash;          // Hashed refresh token (unique)

    private String deviceInfo;         // "Mac Chrome", "iPhone Safari"
    private String ipAddress;
    private String userAgent;

    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant expiresAt;

    private boolean revoked;
    private Instant revokedAt;
    private boolean reuseDetected;

    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------

    public AuthSession(
            Long userId,
            String familyId,
            String tokenHash,
            String deviceInfo,
            String ipAddress,
            String userAgent,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");

        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;

        this.lastUsedAt = this.createdAt;

        this.revoked = false;
        this.revokedAt = null;
        this.reuseDetected = false;
    }

    // For mapping
    public AuthSession() {}

    // ---------------------------------------------------------
    // Domain behavior
    // ---------------------------------------------------------

    /** Mark that this session was used (refresh triggered). */
    public void markUsed(Instant now) {
        this.lastUsedAt = Objects.requireNonNull(now, "now");
    }

    /** Revoke this session. */
    public void revoke(Instant now) {
        Objects.requireNonNull(now, "now");
        if (!this.revoked) {
            this.revoked = true;
            this.revokedAt = now;
        }
    }

    /** Flag reuse attack for this session. */
    public void markReuseDetected() {
        this.reuseDetected = true;
    }

    /** Check if expired. */
    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return expiresAt.isBefore(now);
    }

    // Optional: treat revoked OR reuseDetected as "not usable"
    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now");
        return !revoked && !reuseDetected && !isExpired(now);
    }

    // ---------------------------------------------------------
    // Getters & Setters (needed for mapping)
    // ---------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public boolean isReuseDetected() { return reuseDetected; }
    public void setReuseDetected(boolean reuseDetected) { this.reuseDetected = reuseDetected; }
}