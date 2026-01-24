package com.timeeconomy.auth.domain.signupsession.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class SignupSession {

    private UUID id;

    private String email;
    private boolean emailVerified;
    private boolean emailOtpPending;      // ✅ FACT
    private Instant emailOtpExpiresAt;    // ✅ NEW: OTP expiry (source of truth from Verification)

    private String phoneNumber;
    private boolean phoneVerified;
    private boolean phoneOtpPending;      // ✅ FACT
    private Instant phoneOtpExpiresAt;    // ✅ NEW: OTP expiry

    private String name;
    private String gender;
    private LocalDate birthDate;

    private SignupSessionState state;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;            // ✅ session TTL (separate from OTP TTL)

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
        s.emailOtpExpiresAt = null;

        s.phoneNumber = null;
        s.phoneVerified = false;
        s.phoneOtpPending = false;
        s.phoneOtpExpiresAt = null;

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
        clearEmailOtpFacts();
        clearPhoneOtpFacts();

        touch(now);
        return true;
    }

    // ----------------------------------------------------
    // BIG-CO COMMAND METHODS (one per user intent)
    // ----------------------------------------------------

    /**
     * User intent: "Send / Resend email OTP to this email"
     *
     * @param otpExpiresAt MUST come from Verification's CreateOtp result.
     */
    public void requestEmailOtp(String destination, Instant otpExpiresAt, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        expireOtpFactsIfNeeded(now);

        String next = normalizeEmail(destination);
        if (isBlank(next)) throw new IllegalArgumentException("email is required");
        if (otpExpiresAt == null) throw new IllegalArgumentException("otpExpiresAt is required");

        boolean changed = !Objects.equals(this.email, next);

        if (changed) {
            // destination changed => previous proof no longer valid
            this.email = next;

            this.emailVerified = false;
            clearEmailOtpFacts(); // will be set below

            // policy: keep phone/profile, but phone OTP pending is stale
            clearPhoneOtpFacts();
        }

        // sending/resending always means "waiting for email OTP"
        this.emailVerified = false;
        this.emailOtpPending = true;
        this.emailOtpExpiresAt = otpExpiresAt;

        syncState(now);
    }

    /**
     * Backward-compat overload (temporary).
     * Prefer requestEmailOtp(destination, otpExpiresAt, now).
     */
    @Deprecated
    public void requestEmailOtp(String destination, Instant now) {
        throw new IllegalStateException("Use requestEmailOtp(destination, otpExpiresAt, now)");
    }

    /**
     * User intent: "Send / Resend phone OTP to this phone"
     * Requires emailVerified (big-co guard).
     *
     * @param otpExpiresAt MUST come from Verification's CreateOtp result.
     */
    public void requestPhoneOtp(String destination, Instant otpExpiresAt, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        expireOtpFactsIfNeeded(now);

        if (!emailVerified) {
            throw new IllegalStateException("Cannot send phone OTP: email not verified");
        }

        String next = normalizePhone(destination);
        if (isBlank(next)) throw new IllegalArgumentException("phoneNumber is required");
        if (otpExpiresAt == null) throw new IllegalArgumentException("otpExpiresAt is required");

        boolean changed = !Objects.equals(this.phoneNumber, next);

        if (changed) {
            this.phoneNumber = next;
            this.phoneVerified = false;
            clearPhoneOtpFacts(); // will be set below
        }

        this.phoneVerified = false;
        this.phoneOtpPending = true;
        this.phoneOtpExpiresAt = otpExpiresAt;

        syncState(now);
    }

    /**
     * Backward-compat overload (temporary).
     * Prefer requestPhoneOtp(destination, otpExpiresAt, now).
     */
    @Deprecated
    public void requestPhoneOtp(String destination, Instant now) {
        throw new IllegalStateException("Use requestPhoneOtp(destination, otpExpiresAt, now)");
    }

    /**
     * User intent: "Email OTP verification succeeded"
     */
    public void confirmEmailVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        expireOtpFactsIfNeeded(now);

        if (!emailOtpPending) {
            throw new IllegalStateException("No email OTP pending");
        }
        if (isBlank(email)) {
            throw new IllegalStateException("Email missing");
        }

        this.emailVerified = true;
        clearEmailOtpFacts();

        syncState(now);
    }

    /**
     * User intent: "Phone OTP verification succeeded"
     */
    public void confirmPhoneVerified(Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        expireOtpFactsIfNeeded(now);

        if (!phoneOtpPending) {
            throw new IllegalStateException("No phone OTP pending");
        }
        if (isBlank(phoneNumber)) {
            throw new IllegalStateException("Phone number missing");
        }

        this.phoneVerified = true;
        clearPhoneOtpFacts();

        syncState(now);
    }

    /**
     * User intent: "Submit profile"
     */
    public void submitProfile(String name, String gender, LocalDate birthDate, Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        expireOtpFactsIfNeeded(now);

        if (state != SignupSessionState.PHONE_VERIFIED && state != SignupSessionState.PROFILE_READY) {
            throw new IllegalStateException("Profile not allowed in state=" + state);
        }

        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;

        syncState(now);
    }

    public void markCompleted(Instant now) {
        if (expireIfNeeded(now)) return;
        if (isTerminal()) return;

        expireOtpFactsIfNeeded(now);

        if (state != SignupSessionState.PROFILE_READY) {
            throw new IllegalStateException("Cannot complete unless PROFILE_READY, state=" + state);
        }

        this.state = SignupSessionState.COMPLETED;
        clearEmailOtpFacts();
        clearPhoneOtpFacts();

        touch(now);
        assertInvariants();
    }

    public void cancel(Instant now) {
        if (isTerminal()) return;

        this.state = SignupSessionState.CANCELED;
        clearEmailOtpFacts();
        clearPhoneOtpFacts();

        touch(now);
    }

    // ----------------------------------------------------
    // OTP expiry helpers
    // ----------------------------------------------------

    private void expireOtpFactsIfNeeded(Instant now) {
        // If OTP expired, clear "pending" so Verify flow returns NO_PENDING_OTP cleanly.
        if (emailOtpPending && emailOtpExpiresAt != null && now.isAfter(emailOtpExpiresAt)) {
            clearEmailOtpFacts();
        }
        if (phoneOtpPending && phoneOtpExpiresAt != null && now.isAfter(phoneOtpExpiresAt)) {
            clearPhoneOtpFacts();
        }
    }

    private void clearEmailOtpFacts() {
        this.emailOtpPending = false;
        this.emailOtpExpiresAt = null;
    }

    private void clearPhoneOtpFacts() {
        this.phoneOtpPending = false;
        this.phoneOtpExpiresAt = null;
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

        // pending implies destination exists and not verified and expiry exists
        if (emailOtpPending) {
            if (isBlank(email)) throw new IllegalStateException("emailOtpPending=true but email missing");
            if (emailVerified) throw new IllegalStateException("emailOtpPending=true but emailVerified=true");
            if (emailOtpExpiresAt == null) throw new IllegalStateException("emailOtpPending=true but emailOtpExpiresAt=null");
        } else {
            if (emailOtpExpiresAt != null) throw new IllegalStateException("emailOtpPending=false but emailOtpExpiresAt!=null");
        }

        if (phoneOtpPending) {
            if (isBlank(phoneNumber)) throw new IllegalStateException("phoneOtpPending=true but phoneNumber missing");
            if (phoneVerified) throw new IllegalStateException("phoneOtpPending=true but phoneVerified=true");
            if (phoneOtpExpiresAt == null) throw new IllegalStateException("phoneOtpPending=true but phoneOtpExpiresAt=null");
        } else {
            if (phoneOtpExpiresAt != null) throw new IllegalStateException("phoneOtpPending=false but phoneOtpExpiresAt!=null");
        }

        // state-driven checks (defensive; state is derived anyway)
        switch (state) {
            case DRAFT -> { }
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
            case PHONE_VERIFIED -> {
                if (!emailVerified) throw new IllegalStateException("PHONE_VERIFIED but email not verified");
                if (!phoneVerified) throw new IllegalStateException("PHONE_VERIFIED but phone not verified");
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
        expireOtpFactsIfNeeded(now);
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
            return SignupSessionState.PHONE_VERIFIED;
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

    public Instant getEmailOtpExpiresAt() { return emailOtpExpiresAt; }
    public void setEmailOtpExpiresAt(Instant emailOtpExpiresAt) { this.emailOtpExpiresAt = emailOtpExpiresAt; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public boolean isPhoneOtpPending() { return phoneOtpPending; }
    public void setPhoneOtpPending(boolean phoneOtpPending) { this.phoneOtpPending = phoneOtpPending; }

    public Instant getPhoneOtpExpiresAt() { return phoneOtpExpiresAt; }
    public void setPhoneOtpExpiresAt(Instant phoneOtpExpiresAt) { this.phoneOtpExpiresAt = phoneOtpExpiresAt; }

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