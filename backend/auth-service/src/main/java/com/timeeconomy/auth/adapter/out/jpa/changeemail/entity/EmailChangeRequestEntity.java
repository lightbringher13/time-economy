// src/main/java/com/timeeconomy/auth/adapter/out/jpa/changeemail/entity/EmailChangeRequestEntity.java
package com.timeeconomy.auth.adapter.out.jpa.changeemail.entity;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "email_change_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class EmailChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "old_email", nullable = false, length = 255)
    private String oldEmail;

    @Column(name = "new_email", nullable = false, length = 255)
    private String newEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_factor_type", length = 20)
    private SecondFactorType secondFactorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmailChangeStatus status;

    // ✅ Instant-friendly timestamps (Postgres: TIMESTAMPTZ)
    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}