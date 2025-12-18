package com.timeeconomy.auth.adapter.out.redis.verification;

import java.time.LocalDateTime;

import com.timeeconomy.auth.domain.verification.model.*;

public final class VerificationChallengeSnapshotMapper {
    private VerificationChallengeSnapshotMapper() {}

    public static final int VERSION = 1;

    public static VerificationChallengeSnapshot toSnapshot(VerificationChallenge c) {
        return VerificationChallengeSnapshot.builder()
                .schemaVersion(VERSION)
                .id(n(c.getId()))
                .purpose(n(c.getPurpose().name()))
                .channel(n(c.getChannel().name()))
                .subjectType(n(c.getSubjectType().name()))
                .subjectId(n(c.getSubjectId()))
                .destination(n(c.getDestination()))
                .destinationNorm(n(c.getDestinationNorm()))
                .codeHash(n(c.getCodeHash()))
                .tokenHash(n(c.getTokenHash()))
                .tokenExpiresAt(n(t(c.getTokenExpiresAt())))
                .status(n(c.getStatus().name()))
                .expiresAt(n(t(c.getExpiresAt())))
                .verifiedAt(n(t(c.getVerifiedAt())))
                .consumedAt(n(t(c.getConsumedAt())))
                .attemptCount(String.valueOf(c.getAttemptCount()))
                .maxAttempts(String.valueOf(c.getMaxAttempts()))
                .sentCount(String.valueOf(c.getSentCount()))
                .lastSentAt(n(t(c.getLastSentAt())))
                .requestIp(n(c.getRequestIp()))
                .userAgent(n(c.getUserAgent()))
                .createdAt(n(t(c.getCreatedAt())))
                .updatedAt(n(t(c.getUpdatedAt())))
                .build();
    }

    public static VerificationChallenge toDomain(VerificationChallengeSnapshot s) {
        return new VerificationChallenge(
                blankToNull(s.id()),
                enumOf(s.purpose(), VerificationPurpose.class),
                enumOf(s.channel(), VerificationChannel.class),
                enumOf(s.subjectType(), VerificationSubjectType.class),
                blankToNull(s.subjectId()),
                blankToNull(s.destination()),
                blankToNull(s.destinationNorm()),
                blankToNull(s.codeHash()),
                blankToNull(s.tokenHash()),
                parseTime(s.tokenExpiresAt()),
                enumOf(s.status(), VerificationStatus.class),
                parseTime(s.expiresAt()),
                parseTime(s.verifiedAt()),
                parseTime(s.consumedAt()),
                parseInt(s.attemptCount(), 0),
                parseInt(s.maxAttempts(), 5),
                parseInt(s.sentCount(), 1),
                parseTime(s.lastSentAt()),
                blankToNull(s.requestIp()),
                blankToNull(s.userAgent()),
                parseTime(s.createdAt()),
                parseTime(s.updatedAt())
        );
    }

    public static VerificationChallengeSnapshot upgradeIfNeeded(VerificationChallengeSnapshot s) {
        return s;
    }

    private static String t(LocalDateTime dt) { return dt == null ? null : dt.toString(); }
    private static LocalDateTime parseTime(String v) {
        try { return (v == null || v.isBlank()) ? null : LocalDateTime.parse(v); }
        catch (Exception e) { return null; }
    }
    private static int parseInt(String v, int def) {
        try { return (v == null || v.isBlank()) ? def : Integer.parseInt(v); }
        catch (Exception e) { return def; }
    }
    private static <E extends Enum<E>> E enumOf(String v, Class<E> type) {
        return (v == null || v.isBlank()) ? null : Enum.valueOf(type, v);
    }
    private static String n(String v) { return v == null ? "" : v; }
    private static String blankToNull(String v) { return (v == null || v.isBlank()) ? null : v; }
}