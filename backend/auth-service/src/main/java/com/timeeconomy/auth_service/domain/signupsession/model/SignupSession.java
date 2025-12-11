package com.timeeconomy.auth_service.domain.signupsession.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * SignupSession represents an in-progress signup flow
 * (before an AuthUser exists).
 */
public class SignupSession {

    private UUID id;

    private String email;

    private boolean emailVerified;
    private String phoneNumber;
    private boolean phoneVerified;

    private String name;
    private String gender;
    private LocalDate birthDate;

    private SignupSessionState state;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    // ----------------------------------------------------
    // Constructors
    // ----------------------------------------------------

    /**
     * Constructor for creating a new session in the domain.
     * ID will be generated here (UUID.randomUUID()).
     */
    public static SignupSession createNew(
            String email,
            LocalDateTime now,
            LocalDateTime expiresAt
    ) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        SignupSession session = new SignupSession();
        session.id = UUID.randomUUID();
        session.email = null;
        session.emailVerified = false;
        session.phoneVerified = false;
        session.state = SignupSessionState.EMAIL_PENDING;
        session.createdAt = now;
        session.updatedAt = now;
        session.expiresAt = expiresAt;
        return session;
    }

    // Default constructor for mapper / frameworks
    public SignupSession() {
    }

    // ----------------------------------------------------
    // Domain behavior
    // ----------------------------------------------------

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return state == SignupSessionState.COMPLETED;
    }

    public void markEmailVerified(LocalDateTime now) {
        this.emailVerified = true;
        if (this.state == SignupSessionState.EMAIL_PENDING) {
            this.state = SignupSessionState.EMAIL_VERIFIED;
        }
        this.updatedAt = now;
    }

    public void updateProfile(String name,
                          String gender,
                          LocalDate birthDate,
                          LocalDateTime now) {

        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;

        if (this.emailVerified && this.state == SignupSessionState.EMAIL_VERIFIED) {
            this.state = SignupSessionState.PROFILE_FILLED;
        }

        this.updatedAt = now;
    }

    public void updateEmail(String newEmail, LocalDateTime now) {
        this.email = newEmail;
        this.updatedAt = now;
    }

    public void markCompleted(LocalDateTime now) {
        this.state = SignupSessionState.COMPLETED;
        this.updatedAt = now;
    }

    public void markExpired(LocalDateTime now) {
        this.state = SignupSessionState.EXPIRED;
        this.updatedAt = now;
    }

    // ----------------------------------------------------
    // Getters / Setters
    // ----------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public SignupSessionState getState() { return state; }
    public void setState(SignupSessionState state) { this.state = state; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}