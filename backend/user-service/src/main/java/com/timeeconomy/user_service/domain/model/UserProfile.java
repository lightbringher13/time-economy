package com.timeeconomy.user_service.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserProfile {

    private Long id;                // auth-service의 userId 사용
    private String email;
    private String name;
    private String phoneNumber;
    private UserStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자 (신규 생성용)
    public UserProfile(
            Long id,
            String email,
            String name,
            String phoneNumber,
            UserStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    // 매핑/프레임워크용 기본 생성자
    public UserProfile() {
    }

    // ===== 도메인 행위 (나중에 더 확장 가능) =====

    public void updateProfile(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        touchUpdatedAt();
    }

    public void changeStatus(UserStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus);
        touchUpdatedAt();
    }

    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getter/Setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {    // 필요 시 매핑용
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) { this.email = email; }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}