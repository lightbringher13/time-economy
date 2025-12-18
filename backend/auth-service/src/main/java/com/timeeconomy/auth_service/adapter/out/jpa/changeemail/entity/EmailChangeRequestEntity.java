// src/main/java/com/timeeconomy/auth_service/adapter/out/persistence/entity/EmailChangeRequestEntity.java
package com.timeeconomy.auth_service.adapter.out.jpa.changeemail.entity;

import com.timeeconomy.auth_service.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth_service.domain.changeemail.model.SecondFactorType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_change_requests")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}