package com.timeeconomy.auth.adapter.out.redis.changeemail;

import java.time.LocalDateTime;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

public final class EmailChangeRequestSnapshotMapper {
    private EmailChangeRequestSnapshotMapper() {}

    public static final int VERSION = 1;

    public static EmailChangeRequestSnapshot toSnapshot(EmailChangeRequest r) {
        return EmailChangeRequestSnapshot.builder()
                .schemaVersion(VERSION)
                .id(r.getId() == null ? null : r.getId().toString())
                .userId(r.getUserId() == null ? null : r.getUserId().toString())
                .oldEmail(r.getOldEmail())
                .newEmail(r.getNewEmail())
                .secondFactorType(r.getSecondFactorType() == null ? null : r.getSecondFactorType().name())
                .status(r.getStatus() == null ? null : r.getStatus().name())
                .expiresAt(r.getExpiresAt() == null ? null : r.getExpiresAt().toString())
                .createdAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString())
                .updatedAt(r.getUpdatedAt() == null ? null : r.getUpdatedAt().toString())
                .version(r.getVersion() == null ? null : r.getVersion().toString())
                .build();
    }

    public static EmailChangeRequest toDomain(EmailChangeRequestSnapshot s) {
        return EmailChangeRequest.builder()
                .id(parseLong(s.id()))
                .userId(parseLong(s.userId()))
                .oldEmail(blankToNull(s.oldEmail()))
                .newEmail(blankToNull(s.newEmail()))
                .status(parseEnum(s.status(), EmailChangeStatus.class))
                .secondFactorType(parseEnum(s.secondFactorType(), SecondFactorType.class))
                .expiresAt(parseTime(s.expiresAt()))
                .createdAt(parseTime(s.createdAt()))
                .updatedAt(parseTime(s.updatedAt()))
                .version(parseLong(s.version()))
                .build();
    }

    public static EmailChangeRequestSnapshot upgradeIfNeeded(EmailChangeRequestSnapshot s) {
        if (s.schemaVersion() == 0) {
            return EmailChangeRequestSnapshot.builder()
                    .schemaVersion(VERSION)
                    .id(s.id()).userId(s.userId())
                    .oldEmail(s.oldEmail()).newEmail(s.newEmail())
                    .secondFactorType(s.secondFactorType())
                    .status(s.status())
                    .expiresAt(s.expiresAt())
                    .createdAt(s.createdAt())
                    .updatedAt(s.updatedAt())
                    .version(s.version())
                    .build();
        }
        return s;
    }

    private static String blankToNull(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static Long parseLong(String v) { try { return (v == null || v.isBlank()) ? null : Long.valueOf(v); } catch (Exception e) { return null; } }
    private static LocalDateTime parseTime(String v) { try { return (v == null || v.isBlank()) ? null : LocalDateTime.parse(v); } catch (Exception e) { return null; } }
    private static <E extends Enum<E>> E parseEnum(String v, Class<E> type) {
        try { return (v == null || v.isBlank()) ? null : Enum.valueOf(type, v); } catch (Exception e) { return null; }
    }
}