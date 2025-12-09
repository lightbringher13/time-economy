package com.timeeconomy.auth_service.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model for password reset token (persistence-agnostic).
 */
public class PasswordResetToken {

    private Long id;
    private String email;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------

    /**
     * Factory constructor for NEW tokens (when user clicks "forgot password").
     */
    private PasswordResetToken(String email,
                               String tokenHash,
                               LocalDateTime expiresAt,
                               LocalDateTime createdAt) {
        this.email = Objects.requireNonNull(email, "email must not be null").trim().toLowerCase();
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Factory method used by use case when creating a new token.
     */
    public static PasswordResetToken create(String email,
                                            String tokenHash,
                                            LocalDateTime now,
                                            LocalDateTime expiresAt) {
        return new PasswordResetToken(email, tokenHash, expiresAt, now);
    }

    /**
     * Full reconstruction constructor (for mapper when loading from DB).
     */
    public PasswordResetToken(Long id,
                              String email,
                              String tokenHash,
                              LocalDateTime expiresAt,
                              LocalDateTime usedAt,
                              LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    // ---------------------------------------------------------
    // Domain behavior
    // ---------------------------------------------------------

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(LocalDateTime now) {
        if (this.usedAt == null) {
            this.usedAt = now;
        }
    }

    // ---------------------------------------------------------
    // Getters & Setters (for mapping)
    // ---------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}