package com.timeeconomy.auth.adapter.out.redis.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.verification.model.*;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;

import static com.timeeconomy.auth.adapter.out.redis.verification.VerificationRedisKeys.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Primary
@Component
@RequiredArgsConstructor
public class RedisVerificationChallengeAdapter implements VerificationChallengeRepositoryPort {

    private final StringRedisTemplate redis;

    @Override
    public VerificationChallenge save(VerificationChallenge challenge) {
        LocalDateTime now = LocalDateTime.now();

        String id = challenge.getId();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();   // or ULID if you prefer sortable ids
            challenge.setId(id);
        }

        // expire check (optional guard)
        challenge.markExpiredIfNeeded(now);

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
        LocalDateTime now = LocalDateTime.now();
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

    /**
     * When a challenge becomes non-PENDING, we must:
     * 1) remove the pending pointer SAFELY (do not delete a newer pending)
     * 2) optionally remove verify-index keys for this exact challenge (cleanup)
     */
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
    // TTL policy
    // -------------------------

    private static Duration ttlFrom(VerificationChallenge c, LocalDateTime now) {
        LocalDateTime exp = c.getExpiresAt();

        // if tokenExpiresAt exists and is later, keep record until then
        if (c.getTokenExpiresAt() != null && c.getTokenExpiresAt().isAfter(exp)) {
            exp = c.getTokenExpiresAt();
        }

        Duration d = Duration.between(now, exp);
        if (d.isNegative() || d.isZero()) return Duration.ofSeconds(1);
        return d;
    }

    // -------------------------
    // Hash mapping
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
        m.put("tokenExpiresAt", n(s.tokenExpiresAt()));
        m.put("status", n(s.status()));
        m.put("expiresAt", n(s.expiresAt()));
        m.put("verifiedAt", n(s.verifiedAt()));
        m.put("consumedAt", n(s.consumedAt()));
        m.put("attemptCount", n(s.attemptCount()));
        m.put("maxAttempts", n(s.maxAttempts()));
        m.put("sentCount", n(s.sentCount()));
        m.put("lastSentAt", n(s.lastSentAt()));
        m.put("requestIp", n(s.requestIp()));
        m.put("userAgent", n(s.userAgent()));
        m.put("createdAt", n(s.createdAt()));
        m.put("updatedAt", n(s.updatedAt()));
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
                .tokenExpiresAt(blankToNull(get(raw, "tokenExpiresAt")))
                .status(blankToNull(get(raw, "status")))
                .expiresAt(blankToNull(get(raw, "expiresAt")))
                .verifiedAt(blankToNull(get(raw, "verifiedAt")))
                .consumedAt(blankToNull(get(raw, "consumedAt")))
                .attemptCount(blankToNull(get(raw, "attemptCount")))
                .maxAttempts(blankToNull(get(raw, "maxAttempts")))
                .sentCount(blankToNull(get(raw, "sentCount")))
                .lastSentAt(blankToNull(get(raw, "lastSentAt")))
                .requestIp(blankToNull(get(raw, "requestIp")))
                .userAgent(blankToNull(get(raw, "userAgent")))
                .createdAt(blankToNull(get(raw, "createdAt")))
                .updatedAt(blankToNull(get(raw, "updatedAt")))
                .build();
    }

    private static String get(Map<Object, Object> raw, String k) {
        Object v = raw.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}