// src/main/java/com/timeeconomy/auth_service/domain/model/EmailChangeRequest.java
package com.timeeconomy.auth_service.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailChangeRequest {

    private final Long id;
    private final Long userId;

    private final String oldEmail;
    private final String newEmail;

    private String newEmailCode;

    private SecondFactorType secondFactorType;  // PHONE or OLD_EMAIL
    private String secondFactorCode;

    private EmailChangeStatus status;

    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long version;

    // ========= Factory ==========

    public static EmailChangeRequest create(
            Long userId,
            String oldEmail,
            String newEmail,
            String newEmailCode,
            LocalDateTime expiresAt,
            LocalDateTime now
    ) {
        return EmailChangeRequest.builder()
                .id(null)
                .userId(userId)
                .oldEmail(oldEmail)
                .newEmail(newEmail)
                .newEmailCode(newEmailCode)
                .status(EmailChangeStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    // ========= Business rules / state transitions ==========

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == EmailChangeStatus.PENDING
                || status == EmailChangeStatus.NEW_EMAIL_VERIFIED
                || status == EmailChangeStatus.READY_TO_COMMIT;
    }

    public void verifyNewEmailCode(String code, LocalDateTime now) {
        if (isExpired(now)) {
            this.status = EmailChangeStatus.EXPIRED;
            // optionally throw domain exception
            // throw new EmailChangeExpiredException(...);
            return;
        }
        if (!this.newEmailCode.equals(code)) {
            // throw new InvalidNewEmailCodeException(...);
            return;
        }
        if (status != EmailChangeStatus.PENDING) {
            // throw new InvalidStateTransitionException(...);
            return;
        }
        this.status = EmailChangeStatus.NEW_EMAIL_VERIFIED;
        touch(now);
    }

    public void setSecondFactor(SecondFactorType type, String code, LocalDateTime now) {
        this.secondFactorType = type;
        this.secondFactorCode = code;
        touch(now);
    }

    public void verifySecondFactor(String code, LocalDateTime now) {
        if (isExpired(now)) {
            this.status = EmailChangeStatus.EXPIRED;
            return;
        }
        if (status != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
            return;
        }
        if (this.secondFactorCode == null || !this.secondFactorCode.equals(code)) {
            return;
        }
        this.status = EmailChangeStatus.READY_TO_COMMIT;
        touch(now);
    }

    public void markCompleted(LocalDateTime now) {
        this.status = EmailChangeStatus.COMPLETED;
        touch(now);
    }

    public void markCanceled(LocalDateTime now) {
        this.status = EmailChangeStatus.CANCELED;
        touch(now);
    }

    public void markExpired(LocalDateTime now) {
        this.status = EmailChangeStatus.EXPIRED;
        touch(now);
    }

    private void touch(LocalDateTime now) {
        this.updatedAt = now;
    }

    public void bumpVersion() {
        this.version = (this.version == null ? 0L : this.version + 1L);
    }
}