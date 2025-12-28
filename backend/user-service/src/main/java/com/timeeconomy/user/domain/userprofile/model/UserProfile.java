package com.timeeconomy.user.domain.userprofile.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class UserProfile {

    private Long id;                // auth-service userId (PK)
    private String email;
    private String name;
    private String phoneNumber;
    private UserStatus status;

    private LocalDate birthDate;
    private String gender;

    // ✅ UTC-safe timestamps
    private Instant createdAt;
    private Instant updatedAt;

    // 생성자 (신규 생성용)
    public UserProfile(
            Long id,
            String email,
            String name,
            String phoneNumber,
            UserStatus status,
            LocalDate birthDate,
            String gender,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");

        this.name = name;
        this.phoneNumber = phoneNumber;

        this.status = (status != null ? status : UserStatus.ACTIVE);

        this.birthDate = birthDate;
        this.gender = gender;

        Instant base = (createdAt != null ? createdAt : Instant.now());
        this.createdAt = base;
        this.updatedAt = (updatedAt != null ? updatedAt : base);
    }

    // 매핑/프레임워크용 기본 생성자
    public UserProfile() {}

    // =========================================================
    // ✅ AuthUserRegistered(v1) event support (domain methods)
    // =========================================================

    public static UserProfile createFromAuthUserRegistered(
            Long userId,
            String email,
            String name,
            String phoneNumber,
            LocalDate birthDate,
            String gender,
            Instant occurredAt
    ) {
        Instant now = (occurredAt != null ? occurredAt : Instant.now());

        return new UserProfile(
                userId,
                Objects.requireNonNull(email, "email must not be null"),
                name,
                phoneNumber,
                UserStatus.ACTIVE,
                birthDate,
                gender,
                now,
                now
        );
    }

    public void applyAuthUserRegistered(
            String email,
            String name,
            String phoneNumber,
            LocalDate birthDate,
            String gender,
            Instant occurredAt
    ) {
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.gender = gender;

        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }

        touchUpdatedAt(occurredAt);
    }

    // =========================================================
    // 기존 도메인 행위
    // =========================================================

    public void updateProfile(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        touchUpdatedAt();
    }

    public void applyEmailChangeCommitted(
            String oldEmail,
            String newEmail,
            Instant occurredAt
    ) {
        Objects.requireNonNull(newEmail, "newEmail must not be null");

        if (this.email != null && this.email.equalsIgnoreCase(newEmail)) {
            touchUpdatedAt(occurredAt);
            return;
        }

        if (this.email == null || !this.email.equalsIgnoreCase(oldEmail)) {
            throw new IllegalStateException(
                    "Stale EmailChangeCommitted event. currentEmail=%s, event.oldEmail=%s, event.newEmail=%s"
                            .formatted(this.email, oldEmail, newEmail)
            );
        }

        this.email = newEmail;
        touchUpdatedAt(occurredAt);
    }

    public void updateProfileDetail(String name, String phoneNumber, LocalDate birthDate, String gender) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.gender = gender;
        touchUpdatedAt();
    }

    public void changeStatus(UserStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus);
        touchUpdatedAt();
    }

    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public void touchUpdatedAt(Instant when) {
        this.updatedAt = (when != null ? when : Instant.now());
    }

    // =========================================================
    // Getters/Setters
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}