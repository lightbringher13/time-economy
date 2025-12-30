package com.timeeconomy.auth.adapter.out.redis.changeemail;

import java.time.Instant;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

public final class EmailChangeRequestSnapshotMapper {
    private EmailChangeRequestSnapshotMapper() {}

    public static final int VERSION = 1;

    public static EmailChangeRequestSnapshot toSnapshot(EmailChangeRequest r) {
        return EmailChangeRequestSnapshot.builder()
                .schemaVersion(VERSION)

                .id(r.getId())
                .userId(r.getUserId())

                .oldEmail(r.getOldEmail())
                .newEmail(r.getNewEmail())

                .secondFactorType(r.getSecondFactorType() == null ? null : r.getSecondFactorType().name())
                .status(r.getStatus() == null ? null : r.getStatus().name())

                .expiresAtEpochMillis(toEpochMillis(r.getExpiresAt()))
                .createdAtEpochMillis(toEpochMillis(r.getCreatedAt()))
                .updatedAtEpochMillis(toEpochMillis(r.getUpdatedAt()))

                .version(r.getVersion())
                .build();
    }

    public static EmailChangeRequest toDomain(EmailChangeRequestSnapshot s) {
        return EmailChangeRequest.builder()
                .id(s.id())
                .userId(s.userId())
                .oldEmail(blankToNull(s.oldEmail()))
                .newEmail(blankToNull(s.newEmail()))
                .status(parseEnum(s.status(), EmailChangeStatus.class))
                .secondFactorType(parseEnum(s.secondFactorType(), SecondFactorType.class))
                .expiresAt(toInstant(s.expiresAtEpochMillis()))
                .createdAt(toInstant(s.createdAtEpochMillis()))
                .updatedAt(toInstant(s.updatedAtEpochMillis()))
                .version(s.version())
                .build();
    }

    private static Long toEpochMillis(Instant t) {
        return t == null ? null : t.toEpochMilli();
    }

    private static Instant toInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private static <E extends Enum<E>> E parseEnum(String v, Class<E> type) {
        try { return (v == null || v.isBlank()) ? null : Enum.valueOf(type, v); }
        catch (Exception e) { return null; }
    }
}