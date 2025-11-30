package com.timeeconomy.auth_service.adapter.out.jpa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_session")
public class AuthSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id", nullable = false, length = 100)
    private String familyId;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "reuse_detected", nullable = false)
    private boolean reuseDetected;

    // JPA requires a no-args constructor
    public AuthSessionEntity() {
    }

    public AuthSessionEntity(
            Long userId,
            String familyId,
            String tokenHash,
            String deviceInfo,
            String ipAddress,
            String userAgent,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt,
            LocalDateTime expiresAt,
            boolean revoked,
            LocalDateTime revokedAt,
            boolean reuseDetected
    ) {
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.reuseDetected = reuseDetected;
    }

    // Getters & setters

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

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }

    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }

    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public LocalDateTime getRevokedAt() { return revokedAt; }

    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public boolean isReuseDetected() { return reuseDetected; }

    public void setReuseDetected(boolean reuseDetected) { this.reuseDetected = reuseDetected; }
}