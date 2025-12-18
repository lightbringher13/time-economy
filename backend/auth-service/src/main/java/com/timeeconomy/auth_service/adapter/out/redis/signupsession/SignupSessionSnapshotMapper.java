package com.timeeconomy.auth_service.adapter.out.redis.signupsession;

import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSessionState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class SignupSessionSnapshotMapper {
    private SignupSessionSnapshotMapper() {}

    public static final int VERSION = 1;

    public static SignupSessionSnapshot toSnapshot(SignupSession s) {
        return SignupSessionSnapshot.builder()
                .schemaVersion(VERSION)
                .id(s.getId() == null ? null : s.getId().toString())
                .email(s.getEmail())
                .emailVerified(Boolean.toString(s.isEmailVerified()))
                .phoneNumber(s.getPhoneNumber())
                .phoneVerified(Boolean.toString(s.isPhoneVerified()))
                .name(s.getName())
                .gender(s.getGender())
                .birthDate(s.getBirthDate() == null ? null : s.getBirthDate().toString())
                .state(s.getState() == null ? null : s.getState().name())
                .createdAt(s.getCreatedAt() == null ? null : s.getCreatedAt().toString())
                .updatedAt(s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString())
                .expiresAt(s.getExpiresAt() == null ? null : s.getExpiresAt().toString())
                .build();
    }

    public static SignupSession toDomain(SignupSessionSnapshot snap) {
        SignupSession s = new SignupSession();

        if (hasText(snap.id())) s.setId(UUID.fromString(snap.id()));
        s.setEmail(textOrNull(snap.email()));
        s.setEmailVerified(Boolean.parseBoolean(defaultFalse(snap.emailVerified())));

        s.setPhoneNumber(textOrNull(snap.phoneNumber()));
        s.setPhoneVerified(Boolean.parseBoolean(defaultFalse(snap.phoneVerified())));

        s.setName(textOrNull(snap.name()));
        s.setGender(textOrNull(snap.gender()));

        if (hasText(snap.birthDate())) s.setBirthDate(LocalDate.parse(snap.birthDate()));

        if (hasText(snap.state())) s.setState(SignupSessionState.valueOf(snap.state()));
        else s.setState(SignupSessionState.EMAIL_PENDING);

        if (hasText(snap.createdAt())) s.setCreatedAt(LocalDateTime.parse(snap.createdAt()));
        if (hasText(snap.updatedAt())) s.setUpdatedAt(LocalDateTime.parse(snap.updatedAt()));
        if (hasText(snap.expiresAt())) s.setExpiresAt(LocalDateTime.parse(snap.expiresAt()));

        return s;
    }

    // future-proof hook (v1 -> v2 upgrade can be added here)
    public static SignupSessionSnapshot upgradeIfNeeded(SignupSessionSnapshot snap) {
        int v = snap.schemaVersion();
        if (v == 0) {
            return SignupSessionSnapshot.builder()
                    .schemaVersion(VERSION)
                    .id(snap.id())
                    .email(snap.email())
                    .emailVerified(snap.emailVerified())
                    .phoneNumber(snap.phoneNumber())
                    .phoneVerified(snap.phoneVerified())
                    .name(snap.name())
                    .gender(snap.gender())
                    .birthDate(snap.birthDate())
                    .state(snap.state())
                    .createdAt(snap.createdAt())
                    .updatedAt(snap.updatedAt())
                    .expiresAt(snap.expiresAt())
                    .build();
        }
        return snap;
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
    private static String textOrNull(String s) { return hasText(s) ? s : null; }
    private static String defaultFalse(String s) { return hasText(s) ? s : "false"; }
}