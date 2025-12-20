package com.timeeconomy.user.domain.userprofile.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class UserProfile {

    private Long id;                // auth-service의 userId 사용 (PK로 쓸 예정이면 OK)
    private String email;
    private String name;
    private String phoneNumber;
    private UserStatus status;

    private LocalDate birthDate;
    private String gender;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자 (신규 생성용)
    public UserProfile(
            Long id,
            String email,
            String name,
            String phoneNumber,
            UserStatus status,
            LocalDate birthDate,
            String gender,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");

        this.name = name;
        this.phoneNumber = phoneNumber;

        this.status = (status != null ? status : UserStatus.ACTIVE);

        this.birthDate = birthDate;
        this.gender = gender;

        LocalDateTime base = (createdAt != null ? createdAt : LocalDateTime.now());
        this.createdAt = base;
        this.updatedAt = (updatedAt != null ? updatedAt : base);
    }

    // 매핑/프레임워크용 기본 생성자
    public UserProfile() {}

    // =========================================================
    // ✅ AuthUserRegistered(v1) event support (domain methods)
    // =========================================================

    /**
     * auth.user.registered.v1 이벤트 기반 신규 프로필 생성
     */
    public static UserProfile createFromAuthUserRegistered(
            Long userId,
            String email,
            String name,
            String phoneNumber,
            LocalDate birthDate,
            String gender,
            LocalDateTime occurredAt
    ) {
        LocalDateTime now = (occurredAt != null ? occurredAt : LocalDateTime.now());

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

    /**
     * auth.user.registered.v1 이벤트를 기존 프로필에 반영
     * - Kafka 재처리(중복 소비)에도 안전하도록 "같은 값이면 그대로 덮어씀" 전략
     * - auth-service가 identity source of truth라면 email 동기화는 여기서 수행
     */
    public void applyAuthUserRegistered(
            String email,
            String name,
            String phoneNumber,
            LocalDate birthDate,
            String gender,
            LocalDateTime occurredAt
    ) {
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.gender = gender;

        // 등록 이벤트니까 ACTIVE로 보정 (원하면 제거 가능)
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
            LocalDateTime occurredAt
    ) {
        Objects.requireNonNull(newEmail, "newEmail must not be null");

        // duplicate replay: already applied
        if (this.email != null && this.email.equalsIgnoreCase(newEmail)) {
            touchUpdatedAt(occurredAt);
            return;
        }

        // stale / out-of-order guard
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
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ overload: event timestamp를 쓰고 싶을 때
    public void touchUpdatedAt(LocalDateTime when) {
        this.updatedAt = (when != null ? when : LocalDateTime.now());
    }

    // =========================================================
    // Getter/Setter (가능하면 setter는 점점 줄이는 방향 추천)
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}