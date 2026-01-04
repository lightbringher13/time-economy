package com.timeeconomy.auth.adapter.out.redis.verification;

import java.time.Instant;

import com.timeeconomy.auth.domain.verification.model.*;

public final class VerificationChallengeSnapshotMapper {
    private VerificationChallengeSnapshotMapper() {}

    public static final int VERSION = 1;

    public static VerificationChallengeSnapshot toSnapshot(VerificationChallenge c) {
        return VerificationChallengeSnapshot.builder()
                .schemaVersion(VERSION)
                .id(textOrNull(c.getId()))

                .purpose(c.getPurpose() == null ? null : c.getPurpose().name())
                .channel(c.getChannel() == null ? null : c.getChannel().name())
                .subjectType(c.getSubjectType() == null ? null : c.getSubjectType().name())
                .subjectId(textOrNull(c.getSubjectId()))

                .destination(textOrNull(c.getDestination()))
                .destinationNorm(textOrNull(c.getDestinationNorm()))

                .codeHash(textOrNull(c.getCodeHash()))
                .tokenHash(textOrNull(c.getTokenHash()))

                .status(c.getStatus() == null ? null : c.getStatus().name())

                .expiresAtEpochMillis(toEpochMillis(c.getExpiresAt()))
                .verifiedAtEpochMillis(toEpochMillis(c.getVerifiedAt()))
                .consumedAtEpochMillis(toEpochMillis(c.getConsumedAt()))

                .attemptCount(c.getAttemptCount())
                .maxAttempts(c.getMaxAttempts())
                .sentCount(c.getSentCount())
                .lastSentAtEpochMillis(toEpochMillis(c.getLastSentAt()))

                .requestIp(textOrNull(c.getRequestIp()))
                .userAgent(textOrNull(c.getUserAgent()))

                .createdAtEpochMillis(toEpochMillis(c.getCreatedAt()))
                .updatedAtEpochMillis(toEpochMillis(c.getUpdatedAt()))
                .build();
    }

    public static VerificationChallenge toDomain(VerificationChallengeSnapshot s) {
        return new VerificationChallenge(
                textOrNull(s.id()),
                enumOf(s.purpose(), VerificationPurpose.class),
                enumOf(s.channel(), VerificationChannel.class),
                enumOf(s.subjectType(), VerificationSubjectType.class),
                textOrNull(s.subjectId()),
                textOrNull(s.destination()),
                textOrNull(s.destinationNorm()),
                textOrNull(s.codeHash()),
                textOrNull(s.tokenHash()),
                enumOf(s.status(), VerificationStatus.class),
                toInstant(s.expiresAtEpochMillis()),
                toInstant(s.verifiedAtEpochMillis()),
                toInstant(s.consumedAtEpochMillis()),
                s.attemptCount(),
                s.maxAttempts(),
                s.sentCount(),
                toInstant(s.lastSentAtEpochMillis()),
                textOrNull(s.requestIp()),
                textOrNull(s.userAgent()),
                toInstant(s.createdAtEpochMillis()),
                toInstant(s.updatedAtEpochMillis())
        );
    }

    public static VerificationChallengeSnapshot upgradeIfNeeded(VerificationChallengeSnapshot s) {
        return s; // you said “starting from zero”, so no migration needed
    }

    private static Long toEpochMillis(Instant t) {
        return t == null ? null : t.toEpochMilli();
    }

    private static Instant toInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private static <E extends Enum<E>> E enumOf(String v, Class<E> type) {
        if (v == null || v.isBlank()) return null;
        return Enum.valueOf(type, v);
    }

    private static String textOrNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}