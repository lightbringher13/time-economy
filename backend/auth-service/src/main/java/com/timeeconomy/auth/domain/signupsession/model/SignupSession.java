package com.timeeconomy.auth.domain.signupsession.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class SignupSession {

    private UUID id;

    private String email;
    private boolean emailVerified;
    private boolean emailOtpPending; // ✅ FACT

    private String phoneNumber;
    private boolean phoneVerified;
    private boolean phoneOtpPending; // ✅ FACT

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
        s.emailOtpPending = false;

        s.phoneNumber = null;
        s.phoneVerified = false;
        s.phoneOtpPending = false;

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

        // terminal => clear pendings defensively
        this.emailOtpPending = false;
        this.phoneOtpPending = false;

        touch(now);
        return true;
    }

    // ----------------------------------------------------
    // BIG-CO COMMAND METHODS (one per user intent)
    // ----------------------------------------------------

    /**
     * User intent: "Send / Resend email OTP to this email"
     *
     * Big-co behavior:
     * - Apply destination (if changed)
     * - Reset verification/pending facts properly
     * - Set emailOtpPending=true (we are now waiting for OTP)
     * - State derived from facts
     */
    public void requestEmailOtp(String destination, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        String next = normalizeEmail(destination);
        if (isBlank(next)) throw new IllegalArgumentException("email is required");

        boolean changed = !Objects.equals(this.email, next);

        if (changed) {
            // destination changed => previous proof no longer valid
            this.email = next;

            this.emailVerified = false;
            this.emailOtpPending = false; // will be set true below

            // Policy B: keep phone/profile, but "phone OTP pending" is now stale
            this.phoneOtpPending = false;
        }

        // sending/resending always means "waiting for email OTP"
        this.emailVerified = false;
        this.emailOtpPending = true;

        syncState(now);
    }

    /**
     * User intent: "Send / Resend phone OTP to this phone"
     *
     * Requires emailVerified (big-co guard).
     */
    public void requestPhoneOtp(String destination, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        if (!emailVerified) {
            throw new IllegalStateException("Cannot send phone OTP: email not verified");
        }

        String next = normalizePhone(destination);
        if (isBlank(next)) throw new IllegalArgumentException("phoneNumber is required");

        boolean changed = !Objects.equals(this.phoneNumber, next);

        if (changed) {
            this.phoneNumber = next;
            this.phoneVerified = false;
            this.phoneOtpPending = false; // will be set true below
        }

        this.phoneVerified = false;
        this.phoneOtpPending = true;

        syncState(now);
    }

    /**
     * User intent: "Email OTP verification succeeded"
     */
    public void confirmEmailVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        if (!emailOtpPending) {
            throw new IllegalStateException("No email OTP pending");
        }
        if (isBlank(email)) {
            throw new IllegalStateException("Email missing");
        }

        this.emailVerified = true;
        this.emailOtpPending = false;

        syncState(now);
    }

    /**
     * User intent: "Phone OTP verification succeeded"
     */
    public void confirmPhoneVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        if (!phoneOtpPending) {
            throw new IllegalStateException("No phone OTP pending");
        }
        if (isBlank(phoneNumber)) {
            throw new IllegalStateException("Phone number missing");
        }

        this.phoneVerified = true;
        this.phoneOtpPending = false;

        syncState(now);
    }

    /**
     * User intent: "Submit profile"
     */
    public void submitProfile(String name, String gender, LocalDate birthDate, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        // big-co: allow profile to be submitted once user is at/after profile stage
        if (state != SignupSessionState.PROFILE_PENDING && state != SignupSessionState.PROFILE_READY) {
            throw new IllegalStateException("Profile not allowed in state=" + state);
        }

        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;

        syncState(now);
    }

    /**
     * Called by register flow after user created successfully.
     */
    public void markCompleted(Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        if (state != SignupSessionState.PROFILE_READY) {
            throw new IllegalStateException("Cannot complete unless PROFILE_READY, state=" + state);
        }

        this.state = SignupSessionState.COMPLETED;
        this.emailOtpPending = false;
        this.phoneOtpPending = false;

        touch(now);
        assertInvariants();
    }

    public void cancel(Instant now) {
        if (isTerminal()) return;

        this.state = SignupSessionState.CANCELED;
        this.emailOtpPending = false;
        this.phoneOtpPending = false;

        touch(now);
    }

    // ----------------------------------------------------
    // Invariants (facts + state must be consistent)
    // ----------------------------------------------------

    public void assertInvariants() {
        if (state == null) throw new IllegalStateException("state is null");

        // verified implies destination exists
        if (emailVerified && isBlank(email)) {
            throw new IllegalStateException("emailVerified=true but email is missing");
        }
        if (phoneVerified && isBlank(phoneNumber)) {
            throw new IllegalStateException("phoneVerified=true but phoneNumber is missing");
        }

        // verified implies no pending
        if (emailVerified && emailOtpPending) {
            throw new IllegalStateException("emailVerified=true but emailOtpPending=true");
        }
        if (phoneVerified && phoneOtpPending) {
            throw new IllegalStateException("phoneVerified=true but phoneOtpPending=true");
        }

        // pending implies destination exists and not verified
        if (emailOtpPending) {
            if (isBlank(email)) throw new IllegalStateException("emailOtpPending=true but email missing");
            if (emailVerified) throw new IllegalStateException("emailOtpPending=true but emailVerified=true");
        }
        if (phoneOtpPending) {
            if (isBlank(phoneNumber)) throw new IllegalStateException("phoneOtpPending=true but phoneNumber missing");
            if (phoneVerified) throw new IllegalStateException("phoneOtpPending=true but phoneVerified=true");
        }

        // state-driven checks (defensive; state is derived anyway)
        switch (state) {
            case DRAFT -> {
                // email exists but not verified; pending may be false (draft typing) or true (but then recompute won't produce DRAFT)
            }
            case EMAIL_OTP_SENT -> {
                if (isBlank(email)) throw new IllegalStateException("EMAIL_OTP_SENT but email missing");
                if (emailVerified) throw new IllegalStateException("EMAIL_OTP_SENT but emailVerified=true");
                if (!emailOtpPending) throw new IllegalStateException("EMAIL_OTP_SENT but emailOtpPending=false");
            }
            case EMAIL_VERIFIED -> {
                if (isBlank(email)) throw new IllegalStateException("EMAIL_VERIFIED but email missing");
                if (!emailVerified) throw new IllegalStateException("EMAIL_VERIFIED but emailVerified=false");
                if (emailOtpPending) throw new IllegalStateException("EMAIL_VERIFIED but emailOtpPending=true");
            }
            case PHONE_OTP_SENT -> {
                if (!emailVerified) throw new IllegalStateException("PHONE_OTP_SENT but email not verified");
                if (isBlank(phoneNumber)) throw new IllegalStateException("PHONE_OTP_SENT but phoneNumber missing");
                if (phoneVerified) throw new IllegalStateException("PHONE_OTP_SENT but phoneVerified=true");
                if (!phoneOtpPending) throw new IllegalStateException("PHONE_OTP_SENT but phoneOtpPending=false");
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
    // State derivation (facts → state)
    // ----------------------------------------------------

    private void syncState(Instant now) {
        this.state = recomputeState();
        touch(now);
        assertInvariants();
    }

    private SignupSessionState recomputeState() {
        if (state == SignupSessionState.CANCELED
                || state == SignupSessionState.EXPIRED
                || state == SignupSessionState.COMPLETED) {
            return state;
        }

        if (isBlank(email)) return SignupSessionState.DRAFT;

        if (!emailVerified) {
            return emailOtpPending ? SignupSessionState.EMAIL_OTP_SENT : SignupSessionState.DRAFT;
        }

        if (isBlank(phoneNumber)) return SignupSessionState.EMAIL_VERIFIED;

        if (!phoneVerified) {
            return phoneOtpPending ? SignupSessionState.PHONE_OTP_SENT : SignupSessionState.EMAIL_VERIFIED;
        }

        if (isBlank(name) || isBlank(gender) || birthDate == null) {
            return SignupSessionState.PROFILE_PENDING;
        }

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
    // Getters / Setters
    // ----------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isEmailOtpPending() { return emailOtpPending; }
    public void setEmailOtpPending(boolean emailOtpPending) { this.emailOtpPending = emailOtpPending; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public boolean isPhoneOtpPending() { return phoneOtpPending; }
    public void setPhoneOtpPending(boolean phoneOtpPending) { this.phoneOtpPending = phoneOtpPending; }

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