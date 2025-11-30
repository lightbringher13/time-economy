package com.timeeconomy.auth_service.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Model for authentication sessions / refresh tokens.
 * Pure domain: no JPA, no Spring.
 */
public class AuthSession {

    private Long id;                   // DB primary key
    private Long userId;               // Provided by user-service
    private String familyId;           // Identifies the device family
    private String tokenHash;          // Hashed refresh token

    private String deviceInfo;         // "Mac Chrome", "iPhone Safari"
    private String ipAddress;
    private String userAgent;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;

    private boolean revoked;
    private LocalDateTime revokedAt;
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
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this.userId = Objects.requireNonNull(userId);
        this.familyId = Objects.requireNonNull(familyId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.reuseDetected = false;
    }

    // For JPA or mapping later
    public AuthSession() {}

    // ---------------------------------------------------------
    // Domain behavior
    // ---------------------------------------------------------

    /** Mark that this session was used (refresh triggered). */
    public void markUsed(LocalDateTime now) {
        this.lastUsedAt = now;
    }

    /** Revoke this session. */
    public void revoke(LocalDateTime now) {
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
    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    // ---------------------------------------------------------
    // Getters & Setters (needed for mapping)
    // ---------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public String getFamilyId() { return familyId; }
    public String getTokenHash() { return tokenHash; }

    public String getDeviceInfo() { return deviceInfo; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }

    public boolean isRevoked() { return revoked; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public boolean isReuseDetected() { return reuseDetected; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public void setReuseDetected(boolean reuseDetected) { this.reuseDetected = reuseDetected; }
}