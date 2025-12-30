package com.timeeconomy.auth.adapter.out.redis.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.verification.model.*;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;

import static com.timeeconomy.auth.adapter.out.redis.verification.VerificationRedisKeys.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.time.Clock;

@Primary
@Component
@RequiredArgsConstructor
public class RedisVerificationChallengeAdapter implements VerificationChallengeRepositoryPort {

    private final StringRedisTemplate redis;
    private final Clock clock;

    @Override
    public VerificationChallenge save(VerificationChallenge challenge) {
        Instant now = Instant.now(clock);

        String id = challenge.getId();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            challenge.setId(id);
        }

        // expire check (optional guard)
        challenge.expireIfNeeded(now);

        Duration ttl = ttlFrom(challenge, now);

        // 1) store main hash
        String chKey = ch(id);
        Map<String, String> fields = toHash(VerificationChallengeSnapshotMapper.toSnapshot(challenge));
        redis.opsForHash().putAll(chKey, fields);
        redis.expire(chKey, ttl);

        // 2) maintain "latest" pointer (polling/status)
        indexLatest(challenge, id, ttl);

        // 3) maintain indexes depending on status
        if (challenge.getStatus() == VerificationStatus.PENDING) {
            indexPendingAndVerifyKeys(challenge, id, ttl);
        } else {
            cleanupIndexesForTerminalOrNonPending(challenge, id);
        }

        return challenge;
    }

    @Override
    public Optional<VerificationChallenge> findById(String id) {
        Map<Object, Object> raw = redis.opsForHash().entries(ch(id));
        if (raw == null || raw.isEmpty()) return Optional.empty();

        VerificationChallengeSnapshot snap = VerificationChallengeSnapshotMapper.upgradeIfNeeded(fromHash(raw));
        VerificationChallenge c = VerificationChallengeSnapshotMapper.toDomain(snap);

        // extra guard: if expired pending, delete and return empty
        Instant now = Instant.now(clock);
        if (c.getStatus() == VerificationStatus.PENDING && c.isExpired(now)) {
            redis.delete(ch(id));
            return Optional.empty();
        }

        return Optional.of(c);
    }

    @Override
    public Optional<VerificationChallenge> findActivePending(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel
    ) {
        String pid = redis.opsForValue().get(pending(subjectType, subjectId, purpose, channel));
        if (pid == null || pid.isBlank()) return Optional.empty();

        return findById(pid)
                .filter(c -> c.getStatus() == VerificationStatus.PENDING);
    }

    @Override
    public Optional<VerificationChallenge> findPendingByCodeHash(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String destinationNorm,
            String codeHash
    ) {
        String id = redis.opsForValue().get(otp(subjectType, subjectId, purpose, channel, destinationNorm, codeHash));
        if (id == null || id.isBlank()) return Optional.empty();

        return findById(id)
                .filter(c -> c.getStatus() == VerificationStatus.PENDING)
                .filter(c -> Objects.equals(c.getDestinationNorm(), destinationNorm))
                .filter(c -> Objects.equals(c.getCodeHash(), codeHash));
    }

    @Override
    public Optional<VerificationChallenge> findPendingByTokenHash(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String tokenHash
    ) {
        String id = redis.opsForValue().get(link(subjectType, subjectId, purpose, channel, tokenHash));
        if (id == null || id.isBlank()) return Optional.empty();

        return findById(id)
                .filter(c -> c.getStatus() == VerificationStatus.PENDING)
                .filter(c -> Objects.equals(c.getTokenHash(), tokenHash));
    }

    @Override
    public Optional<VerificationChallenge> findActivePendingByTokenHash(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String tokenHash
    ) {
        String id = redis.opsForValue().get(linkPub(purpose, channel, tokenHash));
        if (id == null || id.isBlank()) return Optional.empty();

        return findById(id)
                .filter(c -> c.getStatus() == VerificationStatus.PENDING)
                .filter(c -> Objects.equals(c.getTokenHash(), tokenHash));
    }

    @Override
    public Optional<VerificationChallenge> findLatestByDestinationNormAndPurpose(
            String destinationNorm,
            VerificationPurpose purpose,
            VerificationChannel channel
    ) {
        String id = redis.opsForValue().get(latest(destinationNorm, purpose, channel));
        if (id == null || id.isBlank()) return Optional.empty();
        return findById(id);
    }

    @Override
    public void put(String challengeId, String rawCode, Duration ttl) {
        redis.opsForValue().set(raw(challengeId), rawCode, ttl);
    }

    @Override
    public Optional<String> getAndDelete(String challengeId) {
        String v = redis.opsForValue().getAndDelete(raw(challengeId));
        return Optional.ofNullable(v);
    }

    // -------------------------
    // Index helpers
    // -------------------------

    private void indexLatest(VerificationChallenge c, String id, Duration ttl) {
        String k = latest(c.getDestinationNorm(), c.getPurpose(), c.getChannel());
        redis.opsForValue().set(k, id, ttl);
    }

    private void indexPendingAndVerifyKeys(VerificationChallenge c, String id, Duration ttl) {
        // enforce one active pending per subject+purpose+channel
        String pk = pending(c.getSubjectType(), c.getSubjectId(), c.getPurpose(), c.getChannel());
        redis.opsForValue().set(pk, id, ttl);

        // OTP lookup index
        if (notBlank(c.getCodeHash())) {
            String ok = otp(
                    c.getSubjectType(), c.getSubjectId(),
                    c.getPurpose(), c.getChannel(),
                    c.getDestinationNorm(), c.getCodeHash()
            );
            redis.opsForValue().set(ok, id, ttl);
        }

        // LINK lookup indexes (subject-known + public)
        if (notBlank(c.getTokenHash())) {
            String lk = link(c.getSubjectType(), c.getSubjectId(), c.getPurpose(), c.getChannel(), c.getTokenHash());
            redis.opsForValue().set(lk, id, ttl);

            String lpk = linkPub(c.getPurpose(), c.getChannel(), c.getTokenHash());
            redis.opsForValue().set(lpk, id, ttl);
        }
    }

    private void cleanupIndexesForTerminalOrNonPending(VerificationChallenge c, String id) {
        // SAFE pending-pointer cleanup (compare-and-delete)
        String pk = pending(c.getSubjectType(), c.getSubjectId(), c.getPurpose(), c.getChannel());
        deleteIfValueMatches(pk, id);

        // Optional cleanup: delete verify index keys for this exact record
        if (notBlank(c.getCodeHash())) {
            redis.delete(otp(
                    c.getSubjectType(), c.getSubjectId(),
                    c.getPurpose(), c.getChannel(),
                    c.getDestinationNorm(), c.getCodeHash()
            ));
        }

        if (notBlank(c.getTokenHash())) {
            redis.delete(link(
                    c.getSubjectType(), c.getSubjectId(),
                    c.getPurpose(), c.getChannel(),
                    c.getTokenHash()
            ));
            redis.delete(linkPub(c.getPurpose(), c.getChannel(), c.getTokenHash()));
        }
    }

    private void deleteIfValueMatches(String key, String expectedValue) {
        String current = redis.opsForValue().get(key);
        if (expectedValue.equals(current)) {
            redis.delete(key);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    // -------------------------
    // TTL policy (Instant)
    // -------------------------

    private static Duration ttlFrom(VerificationChallenge c, Instant now) {
        Instant exp = c.getExpiresAt();

        // keep record until later of (expiresAt, tokenExpiresAt)
        Instant tokenExp = c.getTokenExpiresAt();
        if (tokenExp != null && (exp == null || tokenExp.isAfter(exp))) {
            exp = tokenExp;
        }

        if (exp == null) return Duration.ofHours(24);

        Duration d = Duration.between(now, exp);
        return (d.isNegative() || d.isZero()) ? Duration.ofSeconds(1) : d;
    }

    // -------------------------
    // Hash mapping (epoch millis snapshot)
    // -------------------------

    private static Map<String, String> toHash(VerificationChallengeSnapshot s) {
        Map<String, String> m = new HashMap<>();

        m.put("schemaVersion", String.valueOf(s.schemaVersion()));
        m.put("id", n(s.id()));

        m.put("purpose", n(s.purpose()));
        m.put("channel", n(s.channel()));
        m.put("subjectType", n(s.subjectType()));
        m.put("subjectId", n(s.subjectId()));

        m.put("destination", n(s.destination()));
        m.put("destinationNorm", n(s.destinationNorm()));

        m.put("codeHash", n(s.codeHash()));
        m.put("tokenHash", n(s.tokenHash()));
        m.put("tokenExpiresAtEpochMillis", nLong(s.tokenExpiresAtEpochMillis()));

        m.put("status", n(s.status()));

        m.put("expiresAtEpochMillis", nLong(s.expiresAtEpochMillis()));
        m.put("verifiedAtEpochMillis", nLong(s.verifiedAtEpochMillis()));
        m.put("consumedAtEpochMillis", nLong(s.consumedAtEpochMillis()));

        m.put("attemptCount", String.valueOf(s.attemptCount()));
        m.put("maxAttempts", String.valueOf(s.maxAttempts()));
        m.put("sentCount", String.valueOf(s.sentCount()));
        m.put("lastSentAtEpochMillis", nLong(s.lastSentAtEpochMillis()));

        m.put("requestIp", n(s.requestIp()));
        m.put("userAgent", n(s.userAgent()));

        m.put("createdAtEpochMillis", nLong(s.createdAtEpochMillis()));
        m.put("updatedAtEpochMillis", nLong(s.updatedAtEpochMillis()));

        return m;
    }

    private static VerificationChallengeSnapshot fromHash(Map<Object, Object> raw) {
        return VerificationChallengeSnapshot.builder()
                .schemaVersion(parseInt(get(raw, "schemaVersion"), 0))
                .id(blankToNull(get(raw, "id")))

                .purpose(blankToNull(get(raw, "purpose")))
                .channel(blankToNull(get(raw, "channel")))
                .subjectType(blankToNull(get(raw, "subjectType")))
                .subjectId(blankToNull(get(raw, "subjectId")))

                .destination(blankToNull(get(raw, "destination")))
                .destinationNorm(blankToNull(get(raw, "destinationNorm")))

                .codeHash(blankToNull(get(raw, "codeHash")))
                .tokenHash(blankToNull(get(raw, "tokenHash")))
                .tokenExpiresAtEpochMillis(parseLongObj(get(raw, "tokenExpiresAtEpochMillis")))

                .status(blankToNull(get(raw, "status")))

                .expiresAtEpochMillis(parseLongObj(get(raw, "expiresAtEpochMillis")))
                .verifiedAtEpochMillis(parseLongObj(get(raw, "verifiedAtEpochMillis")))
                .consumedAtEpochMillis(parseLongObj(get(raw, "consumedAtEpochMillis")))

                .attemptCount(parseInt(get(raw, "attemptCount"), 0))
                .maxAttempts(parseInt(get(raw, "maxAttempts"), 5))
                .sentCount(parseInt(get(raw, "sentCount"), 1))
                .lastSentAtEpochMillis(parseLongObj(get(raw, "lastSentAtEpochMillis")))

                .requestIp(blankToNull(get(raw, "requestIp")))
                .userAgent(blankToNull(get(raw, "userAgent")))

                .createdAtEpochMillis(parseLongObj(get(raw, "createdAtEpochMillis")))
                .updatedAtEpochMillis(parseLongObj(get(raw, "updatedAtEpochMillis")))
                .build();
    }

    private static String get(Map<Object, Object> raw, String k) {
        Object v = raw.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private static int parseInt(String s, int def) {
        try { return (s == null || s.isBlank()) ? def : Integer.parseInt(s); }
        catch (Exception e) { return def; }
    }

    private static Long parseLongObj(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.valueOf(s); }
        catch (Exception e) { return null; }
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String nLong(Long v) { return v == null ? "" : String.valueOf(v); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}