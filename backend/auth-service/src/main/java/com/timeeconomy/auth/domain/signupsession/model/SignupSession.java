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
                // DRAFT just means "not ready to proceed yet"
                // do NOT require emailVerified/phoneVerified to be false
            }
            case EMAIL_OTP_SENT -> {
                if (isBlank(email)) throw new IllegalStateException("EMAIL_OTP_SENT but email missing");
                if (emailVerified) throw new IllegalStateException("EMAIL_OTP_SENT but emailVerified=true");
            }
            case EMAIL_VERIFIED -> {
                if (isBlank(email)) throw new IllegalStateException("EMAIL_VERIFIED but email missing");
                if (!emailVerified) throw new IllegalStateException("EMAIL_VERIFIED but emailVerified=false");
            }
            case PHONE_OTP_SENT -> {
                if (!emailVerified) throw new IllegalStateException("PHONE_OTP_SENT but email not verified");
                if (isBlank(phoneNumber)) throw new IllegalStateException("PHONE_OTP_SENT but phoneNumber missing");
                if (phoneVerified) throw new IllegalStateException("PHONE_OTP_SENT but phoneVerified=true");
            }
            case PHONE_VERIFIED -> {
                // honestly you don't need PHONE_VERIFIED at all if you always recompute;
                // but keep it if enum has it.
                if (!phoneVerified) throw new IllegalStateException("PHONE_VERIFIED but phoneVerified=false");
            }
            case PROFILE_PENDING -> {
                if (!emailVerified) throw new IllegalStateException("PROFILE_PENDING but email not verified");
                if (!phoneVerified) throw new IllegalStateException("PROFILE_PENDING but phone not verified");
            }
            case PROFILE_READY -> {
                if (!emailVerified) throw new IllegalStateException("PROFILE_READY but email not verified");
                if (!phoneVerified) throw new IllegalStateException("PROFILE_READY but phone not verified");
                if (isBlank(name)) throw new IllegalStateException("PROFILE_READY but name missing");
                if (isBlank(gender)) throw new IllegalStateException("PROFILE_READY but gender missing");
                if (birthDate == null) throw new IllegalStateException("PROFILE_READY but birthDate missing");
            }
            case COMPLETED -> {
                if (!emailVerified) throw new IllegalStateException("COMPLETED but email not verified");
                if (!phoneVerified) throw new IllegalStateException("COMPLETED but phone not verified");
            }
            case CANCELED, EXPIRED -> { }
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
        if (isBlank(email)) throw new IllegalStateException("Cannot send email OTP: email missing");

        this.emailVerified = false;
        this.state = SignupSessionState.EMAIL_OTP_SENT;
        syncState(now);
    }

    public void markEmailVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        // You can keep the guard if you want:
        if (state != SignupSessionState.EMAIL_OTP_SENT) return;

        this.emailVerified = true;
        syncState(now); // ✅ this may become EMAIL_VERIFIED or PROFILE_PENDING/READY
    }

    public void editEmail(String newEmail, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        this.email = normalizeEmail(newEmail);
        this.emailVerified = false;

        // ✅ DO NOT delete phone/profile.
        // Leave phoneVerified/profile as-is so recompute can restore later.

        // If you want to force the user into email step UI:
        this.state = SignupSessionState.DRAFT;

        syncState(now);
    }

    // ----------------------------------------------------
    // Phone OTP flow
    // ----------------------------------------------------

    public void markPhoneOtpSent(Instant now) {
        if (expireIfNeeded(now)) return;
        if (!emailVerified) throw new IllegalStateException("Cannot send phone OTP: email not verified");
        if (isBlank(phoneNumber)) throw new IllegalStateException("Cannot send phone OTP: phoneNumber missing");

        this.phoneVerified = false;
        this.state = SignupSessionState.PHONE_OTP_SENT;
        syncState(now);
    }

    public void markPhoneVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.PHONE_OTP_SENT) return;

        this.phoneVerified = true;
        syncState(now); // ✅ becomes PROFILE_PENDING or PROFILE_READY if profile already exists
    }

    public void editPhone(String newPhone, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;
        if (!emailVerified) return;

        this.phoneNumber = normalizePhone(newPhone);
        this.phoneVerified = false;

        // ✅ DO NOT delete profile
        this.state = SignupSessionState.EMAIL_VERIFIED; // or DRAFT; recompute will fix anyway

        syncState(now);
    }

    // ----------------------------------------------------
    // Profile + complete
    // ----------------------------------------------------

    public void submitProfile(String name, String gender, LocalDate birthDate, Instant now) {
        if (expireIfNeeded(now)) return;

        // you can keep guard if you want:
        if (state != SignupSessionState.PROFILE_PENDING && state != SignupSessionState.PROFILE_READY) return;

        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;

        syncState(now); // ✅ becomes PROFILE_READY only if both verified
    }

    /** Called by RegisterService after user row created */
    public void markCompleted(Instant now) {
        if (expireIfNeeded(now)) return;
        if (state != SignupSessionState.PROFILE_READY) return;

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

    private void syncState(Instant now) {
        this.state = recomputeState();
        touch(now);
        assertInvariants();
    }

    /**
     * Derive state from facts.
     *
     * Since you don't store "otpSentAt", we use the current state as a hint:
     * - if we're already in EMAIL_OTP_SENT and email still unverified -> stay EMAIL_OTP_SENT
     * - otherwise, fall back to DRAFT (send code step)
     * Same for PHONE_OTP_SENT.
     */
    private SignupSessionState recomputeState() {
        // terminal states stay terminal
        if (state == SignupSessionState.CANCELED
                || state == SignupSessionState.EXPIRED
                || state == SignupSessionState.COMPLETED) {
            return state;
        }

        // 1) email missing -> DRAFT
        if (isBlank(email)) return SignupSessionState.DRAFT;

        // 2) email not verified -> EMAIL_OTP_SENT if we were already waiting for OTP, else DRAFT
        if (!emailVerified) {
            return (state == SignupSessionState.EMAIL_OTP_SENT)
                    ? SignupSessionState.EMAIL_OTP_SENT
                    : SignupSessionState.DRAFT;
        }

        // 3) email verified but phone missing -> EMAIL_VERIFIED
        if (isBlank(phoneNumber)) return SignupSessionState.EMAIL_VERIFIED;

        // 4) phone not verified -> PHONE_OTP_SENT if we were already waiting for OTP, else EMAIL_VERIFIED
        if (!phoneVerified) {
            return (state == SignupSessionState.PHONE_OTP_SENT)
                    ? SignupSessionState.PHONE_OTP_SENT
                    : SignupSessionState.EMAIL_VERIFIED;
        }

        // 5) both verified but profile incomplete -> PROFILE_PENDING
        if (isBlank(name) || isBlank(gender) || birthDate == null) {
            return SignupSessionState.PROFILE_PENDING;
        }

        // 6) everything present -> PROFILE_READY
        return SignupSessionState.PROFILE_READY;
    }

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