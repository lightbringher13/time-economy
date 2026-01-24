package com.timeeconomy.auth.adapter.out.signupsession.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public final class SignupSessionSnapshotMapper {
    private SignupSessionSnapshotMapper() {}

    public static final int VERSION = 3; // ✅ bump schema version again (otp expiresAt added)

    public static SignupSessionSnapshot toSnapshot(SignupSession s) {
        return SignupSessionSnapshot.builder()
                .schemaVersion(VERSION)
                .id(s.getId() == null ? null : s.getId().toString())

                .email(s.getEmail())
                .emailVerified(s.isEmailVerified())
                .emailOtpPending(s.isEmailOtpPending())
                .emailOtpExpiresAtEpochMillis(toEpochMillis(s.getEmailOtpExpiresAt())) // ✅ NEW

                .phoneNumber(s.getPhoneNumber())
                .phoneVerified(s.isPhoneVerified())
                .phoneOtpPending(s.isPhoneOtpPending())
                .phoneOtpExpiresAtEpochMillis(toEpochMillis(s.getPhoneOtpExpiresAt())) // ✅ NEW

                .name(s.getName())
                .gender(s.getGender())
                .birthDateEpochDays(s.getBirthDate() == null ? null : (int) s.getBirthDate().toEpochDay())

                .state(s.getState() == null ? null : s.getState().name())

                .createdAtEpochMillis(toEpochMillis(s.getCreatedAt()))
                .updatedAtEpochMillis(toEpochMillis(s.getUpdatedAt()))
                .expiresAtEpochMillis(toEpochMillis(s.getExpiresAt()))
                .build();
    }

    public static SignupSession toDomain(SignupSessionSnapshot snap) {
        SignupSession s = new SignupSession();

        if (hasText(snap.id())) s.setId(UUID.fromString(snap.id()));

        s.setEmail(textOrNull(snap.email()));
        s.setEmailVerified(snap.emailVerified());
        s.setEmailOtpPending(snap.emailOtpPending());

        // ✅ NEW (old snapshots just deserialize null here)
        s.setEmailOtpExpiresAt(toInstant(snap.emailOtpExpiresAtEpochMillis()));

        s.setPhoneNumber(textOrNull(snap.phoneNumber()));
        s.setPhoneVerified(snap.phoneVerified());
        s.setPhoneOtpPending(snap.phoneOtpPending());

        // ✅ NEW
        s.setPhoneOtpExpiresAt(toInstant(snap.phoneOtpExpiresAtEpochMillis()));

        s.setName(textOrNull(snap.name()));
        s.setGender(textOrNull(snap.gender()));

        if (snap.birthDateEpochDays() != null) {
            s.setBirthDate(LocalDate.ofEpochDay(snap.birthDateEpochDays()));
        }

        // ✅ default state = DRAFT if missing
        if (hasText(snap.state())) {
            s.setState(SignupSessionState.valueOf(snap.state()));
        } else {
            s.setState(SignupSessionState.DRAFT);
        }

        s.setCreatedAt(toInstant(snap.createdAtEpochMillis()));
        s.setUpdatedAt(toInstant(snap.updatedAtEpochMillis()));
        s.setExpiresAt(toInstant(snap.expiresAtEpochMillis()));

        // ✅ fail fast if Redis snapshot is inconsistent
        s.assertInvariants();

        return s;
    }

    private static Long toEpochMillis(Instant t) {
        return t == null ? null : t.toEpochMilli();
    }

    private static Instant toInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
    private static String textOrNull(String s) { return hasText(s) ? s : null; }
}