// src/main/java/com/timeeconomy/auth_service/domain/changeemail/model/EmailChangeRequest.java
package com.timeeconomy.auth.domain.changeemail.model;

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

    private SecondFactorType secondFactorType;

    private EmailChangeStatus status;

    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long version;

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
                .status(EmailChangeStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == EmailChangeStatus.PENDING
                || status == EmailChangeStatus.NEW_EMAIL_VERIFIED
                || status == EmailChangeStatus.READY_TO_COMMIT;
    }

    // =========================================
    // NEW: state transitions driven by challenges
    // =========================================

    public void markNewEmailVerified(LocalDateTime now) {
        if (isExpired(now)) {
            this.status = EmailChangeStatus.EXPIRED;
            touch(now);
            return;
        }
        if (status != EmailChangeStatus.PENDING) {
            return;
        }
        this.status = EmailChangeStatus.NEW_EMAIL_VERIFIED;
        touch(now);
    }

    public void setSecondFactorType(SecondFactorType type, LocalDateTime now) {
        this.secondFactorType = type;
        touch(now);
    }

    public void markReadyToCommit(LocalDateTime now) {
        if (isExpired(now)) {
            this.status = EmailChangeStatus.EXPIRED;
            touch(now);
            return;
        }
        if (status != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
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