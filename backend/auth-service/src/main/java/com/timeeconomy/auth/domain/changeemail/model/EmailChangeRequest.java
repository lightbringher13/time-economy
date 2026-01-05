package com.timeeconomy.auth.domain.changeemail.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EmailChangeRequest {

    private final Long id;
    private final Long userId;

    private final String oldEmail;
    private final String newEmail;

    private SecondFactorType secondFactorType;

    private EmailChangeStatus status;

    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private Long version;

    public static EmailChangeRequest create(
            Long userId,
            String oldEmail,
            String newEmail,
            String newEmailCode, // (currently unused; keep if API compatibility matters)
            Instant expiresAt,
            Instant now
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

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == EmailChangeStatus.PENDING
                || status == EmailChangeStatus.NEW_EMAIL_VERIFIED
                || status == EmailChangeStatus.SECOND_FACTOR_PENDING
                || status == EmailChangeStatus.READY_TO_COMMIT;
    }

    // =========================================
    // state transitions driven by challenges
    // =========================================

    public void markNewEmailVerified(Instant now) {
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

    public void setSecondFactorType(SecondFactorType type, Instant now) {
        this.secondFactorType = type;
        touch(now);
    }

    public void markSecondFactorPending(Instant now) {
        if (isExpired(now)) {
            this.status = EmailChangeStatus.EXPIRED;
            touch(now);
            return;
        }
        if (status != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
            return;
        }
        this.status = EmailChangeStatus.SECOND_FACTOR_PENDING;
        touch(now);
    }

    public void markReadyToCommit(Instant now) {
        if (isExpired(now)) {
            this.status = EmailChangeStatus.EXPIRED;
            touch(now);
            return;
        }
        if (status != EmailChangeStatus.SECOND_FACTOR_PENDING) {
            return;
        }
        this.status = EmailChangeStatus.READY_TO_COMMIT;
        touch(now);
    }

    public void markCompleted(Instant now) {
        this.status = EmailChangeStatus.COMPLETED;
        touch(now);
    }

    public void markCanceled(Instant now) {
        this.status = EmailChangeStatus.CANCELED;
        touch(now);
    }

    public void markExpired(Instant now) {
        this.status = EmailChangeStatus.EXPIRED;
        touch(now);
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    public void bumpVersion() {
        this.version = (this.version == null ? 0L : this.version + 1L);
    }
}