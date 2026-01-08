package com.timeeconomy.auth.domain.signupsession.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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

    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;

    // ----------------------------------------------------
    // Factory
    // ----------------------------------------------------

    public static SignupSession createNew(Instant now, Instant expiresAt) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        SignupSession s = new SignupSession();
        s.id = UUID.randomUUID();

        s.email = null;
        s.emailVerified = false;

        s.phoneNumber = null;
        s.phoneVerified = false;

        s.name = null;
        s.gender = null;
        s.birthDate = null;

        s.state = SignupSessionState.DRAFT;

        s.createdAt = now;
        s.updatedAt = now;
        s.expiresAt = expiresAt;

        s.assertInvariants();
        return s;
    }

    public SignupSession() {}

    // ----------------------------------------------------
    // Expiry / terminal
    // ----------------------------------------------------

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isTerminal() {
        return state == SignupSessionState.COMPLETED
                || state == SignupSessionState.CANCELED
                || state == SignupSessionState.EXPIRED;
    }

    public boolean expireIfNeeded(Instant now) {
        if (isTerminal()) return false;
        if (!isExpired(now)) return false;

        this.state = SignupSessionState.EXPIRED;
        touch(now);
        return true;
    }

    // ----------------------------------------------------
    // Invariants (keep state + booleans consistent)
    // ----------------------------------------------------

    public void assertInvariants() {
        if (state == null) throw new IllegalStateException("state is null");

        // email verified implies email exists
        if (emailVerified && isBlank(email)) {
            throw new IllegalStateException("emailVerified=true but email is missing");
        }

        // phone verified implies phone exists
        if (phoneVerified && isBlank(phoneNumber)) {
            throw new IllegalStateException("phoneVerified=true but phoneNumber is missing");
        }

        // state-driven checks
        switch (state) {
            case DRAFT -> {
                // booleans must be false
                if (emailVerified) throw new IllegalStateException("DRAFT but emailVerified=true");
                if (phoneVerified) throw new IllegalStateException("DRAFT but phoneVerified=true");
            }
            case EMAIL_OTP_SENT -> {
                if (isBlank(email)) throw new IllegalStateException("EMAIL_OTP_SENT but email missing");
                if (emailVerified) throw new IllegalStateException("EMAIL_OTP_SENT but emailVerified=true");
                if (phoneVerified) throw new IllegalStateException("EMAIL_OTP_SENT but phoneVerified=true");
            }
            case EMAIL_VERIFIED -> {
                if (isBlank(email)) throw new IllegalStateException("EMAIL_VERIFIED but email missing");
                if (!emailVerified) throw new IllegalStateException("EMAIL_VERIFIED but emailVerified=false");
                // phone may or may not exist yet, but cannot be verified before phone OTP flow
                if (phoneVerified) {
                    throw new IllegalStateException("EMAIL_VERIFIED but phoneVerified=true (invalid ordering)");
                }
            }
            case PHONE_OTP_SENT -> {
                if (!emailVerified) throw new IllegalStateException("PHONE_OTP_SENT but email not verified");
                if (isBlank(phoneNumber)) throw new IllegalStateException("PHONE_OTP_SENT but phoneNumber missing");
                if (phoneVerified) throw new IllegalStateException("PHONE_OTP_SENT but phoneVerified=true");
            }
            case PHONE_VERIFIED -> {
                if (!emailVerified) throw new IllegalStateException("PHONE_VERIFIED but email not verified");
                if (!phoneVerified) throw new IllegalStateException("PHONE_VERIFIED but phoneVerified=false");
            }
            case PROFILE_PENDING -> {
                if (!emailVerified) throw new IllegalStateException("PROFILE_PENDING but email not verified");
                if (!phoneVerified) throw new IllegalStateException("PROFILE_PENDING but phone not verified");
            }
            case COMPLETED -> {
                if (!emailVerified) throw new IllegalStateException("COMPLETED but email not verified");
                if (!phoneVerified) throw new IllegalStateException("COMPLETED but phone not verified");
            }
            case CANCELED, EXPIRED -> {
                // nothing extra
            }
        }
    }

    // ----------------------------------------------------
    // DRAFT: user typing
    // ----------------------------------------------------

    /** Allowed while DRAFT only: set email before sending OTP */
    public void setDraftEmail(String email, Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.DRAFT) return;

        this.email = normalizeEmail(email);
        this.emailVerified = false;
        touch(now);
        assertInvariants();
    }

    /** Allowed while DRAFT only: set phone before sending OTP */
    public void setDraftPhone(String phoneNumber, Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.DRAFT) return;

        this.phoneNumber = normalizePhone(phoneNumber);
        this.phoneVerified = false;
        touch(now);
        assertInvariants();
    }

    // ----------------------------------------------------
    // Email OTP flow
    // ----------------------------------------------------

    /** UI action: "Send email code" */
    public void markEmailOtpSent(Instant now) {
        if (expireIfNeeded(now)) return;

        // allow sending from DRAFT or re-sending while EMAIL_OTP_SENT
        if (state != SignupSessionState.DRAFT && state != SignupSessionState.EMAIL_OTP_SENT) return;

        if (isBlank(email)) throw new IllegalStateException("Cannot send email OTP: email missing");

        this.emailVerified = false;
        this.state = SignupSessionState.EMAIL_OTP_SENT;
        touch(now);
        assertInvariants();
    }

    /** UI action: "Verify email code" */
    public void markEmailVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.EMAIL_OTP_SENT) return;

        this.emailVerified = true;
        this.state = SignupSessionState.EMAIL_VERIFIED;
        touch(now);
        assertInvariants();
    }

    /** Back/Edit: user wants to change email after OTP sent or verified */
    public void editEmail(String newEmail, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        // allow editing anytime before completion
        this.email = normalizeEmail(newEmail);
        this.emailVerified = false;

        // editing email invalidates everything downstream
        this.phoneVerified = false;
        this.phoneNumber = null;

        this.name = null;
        this.gender = null;
        this.birthDate = null;

        this.state = SignupSessionState.DRAFT;
        touch(now);
        assertInvariants();
    }

    // ----------------------------------------------------
    // Phone OTP flow
    // ----------------------------------------------------

    /** UI action: "Send SMS code" */
    public void markPhoneOtpSent(Instant now) {
        if (expireIfNeeded(now)) return;

        // allow sending from EMAIL_VERIFIED, or resending while PHONE_OTP_SENT
        if (state != SignupSessionState.EMAIL_VERIFIED && state != SignupSessionState.PHONE_OTP_SENT) return;

        if (!emailVerified) throw new IllegalStateException("Cannot send phone OTP: email not verified");
        if (isBlank(phoneNumber)) throw new IllegalStateException("Cannot send phone OTP: phoneNumber missing");

        this.phoneVerified = false;
        this.state = SignupSessionState.PHONE_OTP_SENT;
        touch(now);
        assertInvariants();
    }

    /** UI action: "Verify SMS code" */
    public void markPhoneVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.PHONE_OTP_SENT) return;

        this.phoneVerified = true;
        this.state = SignupSessionState.PHONE_VERIFIED;

        // after phone verified, next UI step is profile
        this.state = SignupSessionState.PROFILE_PENDING;

        touch(now);
        assertInvariants();
    }

    /** Back/Edit: user wants to change phone after SMS sent/verified */
    public void editPhone(String newPhone, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        // only sensible after email verified
        if (!emailVerified) return;

        this.phoneNumber = normalizePhone(newPhone);
        this.phoneVerified = false;

        // changing phone invalidates profile (downstream)
        this.name = null;
        this.gender = null;
        this.birthDate = null;

        // go back to “need phone OTP”
        this.state = SignupSessionState.EMAIL_VERIFIED;
        touch(now);
        assertInvariants();
    }

    // ----------------------------------------------------
    // Profile + complete
    // ----------------------------------------------------

    public void submitProfile(String name, String gender, LocalDate birthDate, Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.PROFILE_PENDING) return;

        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;

        touch(now);
        assertInvariants();
    }

    /** Called by RegisterService after user row created */
    public void markCompleted(Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.PROFILE_PENDING) return;

        // profile should already be filled, but assertInvariants will enforce it if you want to require it.
        this.state = SignupSessionState.COMPLETED;
        touch(now);
        assertInvariants();
    }

    public void cancel(Instant now) {
        if (isTerminal()) return;
        this.state = SignupSessionState.CANCELED;
        touch(now);
    }

    // ----------------------------------------------------
    // Helpers
    // ----------------------------------------------------

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.trim(); // TODO: E.164 normalize later
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ----------------------------------------------------
    // Getters / Setters (keep for mapper/framework, but prefer domain methods)
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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}