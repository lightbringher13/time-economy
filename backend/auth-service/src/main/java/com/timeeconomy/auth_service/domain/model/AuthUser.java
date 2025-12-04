package com.timeeconomy.auth_service.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Auth 도메인의 사용자 계정 모델.
 *
 * - id: auth 서비스 내부 PK
 * - userId: 외부 도메인(user-service 등)에서 사용하는 유저 ID (옵션)
 */
public class AuthUser {

    // auth_user.id (auth 서비스 내부 PK)
    private Long id;

    // auth_user.user_id (외부 도메인 유저 ID, 아직 안 써도 됨)
    private Long userId;

    // 로그인 계정 정보
    private String email;
    private String passwordHash;
    private AuthStatus status;

    // 보안/상태
    private int failedLoginAttempts;
    private LocalDateTime lockedAt;
    private LocalDateTime lastLoginAt;

    // 연락처 + 인증 상태
    private String phoneNumber;
    private boolean emailVerified;
    private boolean phoneVerified;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------

    /**
     * 신규 유저 생성용 생성자.
     * userId(외부 유저 ID)는 나중에 연동 시점에 set 할 수 있음.
     * phoneNumber는 선택적으로 받을 수 있음.
     */
    public AuthUser(String email, String passwordHash, String phoneNumber) {
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.phoneNumber = phoneNumber; // nullable 허용

        this.status = AuthStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.emailVerified = false;
        this.phoneVerified = false;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 기존 코드 호환용 생성자 (phoneNumber 없이).
     * 내부적으로 phoneNumber = null 로 설정.
     */
    public AuthUser(String email, String passwordHash) {
        this(email, passwordHash, null);
    }

    // for mapper / JPA 어댑터
    public AuthUser() {
    }

    // ---------------------------------------------------------
    // Domain behavior
    // ---------------------------------------------------------

    public boolean isActive() {
        return this.status == AuthStatus.ACTIVE;
    }

    /**
     * 로그인 성공 처리:
     * - 실패 카운트 초기화
     * - lockedAt 초기화
     * - lastLoginAt / updatedAt 갱신
     */
    public void markLoginSuccess(LocalDateTime now) {
        this.failedLoginAttempts = 0;
        this.lockedAt = null;
        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    /**
     * 로그인 실패 처리:
     * - 실패 카운트 증가
     * - 임계 횟수 초과 시 계정 잠금
     */
    public void markLoginFailure(LocalDateTime now, int maxAttemptsBeforeLock) {
        this.failedLoginAttempts++;
        this.updatedAt = now;

        if (this.failedLoginAttempts >= maxAttemptsBeforeLock && this.status == AuthStatus.ACTIVE) {
            this.status = AuthStatus.LOCKED;
            this.lockedAt = now;
        }
    }

    public void activate(LocalDateTime now) {
        this.status = AuthStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.lockedAt = null;
        this.updatedAt = now;
    }

    public void lock(LocalDateTime now) {
        this.status = AuthStatus.LOCKED;
        this.lockedAt = now;
        this.updatedAt = now;
    }

    public void markDeleted(LocalDateTime now) {
        this.status = AuthStatus.DELETED;
        this.updatedAt = now;
    }

    /**
     * 이메일 인증 완료 처리
     */
    public void markEmailVerified(LocalDateTime now) {
        this.emailVerified = true;
        this.updatedAt = now;
    }

    /**
     * 휴대전화 인증 완료 처리
     */
    public void markPhoneVerified(LocalDateTime now) {
        this.phoneVerified = true;
        this.updatedAt = now;
    }

    // ---------------------------------------------------------
    // Getters & Setters (for mapping)
    // ---------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public AuthStatus getStatus() { return status; }
    public void setStatus(AuthStatus status) { this.status = status; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}