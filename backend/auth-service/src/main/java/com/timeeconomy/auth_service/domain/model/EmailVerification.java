package com.timeeconomy.auth_service.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class EmailVerification {

    private Long id;
    private String email;
    private String code; // raw code or hashed? → domain은 raw 적어두고 저장은 adapter에서 hash
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    public EmailVerification(
            Long id,
            String email,
            String code,
            LocalDateTime expiresAt,
            LocalDateTime verifiedAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.email = Objects.requireNonNull(email);
        this.code = Objects.requireNonNull(code);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.verifiedAt = verifiedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    // 생성 시 사용 convenience constructor
    public EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this(null, email, code, expiresAt, null, LocalDateTime.now());
    }

    // ===== BEHAVIOR =====

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void markVerified(LocalDateTime now) {
        this.verifiedAt = now;
    }

    // ===== GETTERS / SETTERS =====

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }
}