package com.timeeconomy.auth.adapter.out.redis.changeemail;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailChangeRequestRedisStore {

    private final StringRedisTemplate redis;

    public void upsert(EmailChangeRequest req, Instant now) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("Redis store requires non-null request id");
        }

        String rk = EmailChangeRedisKeys.requestKey(req.getId());

        EmailChangeRequestSnapshot snap = EmailChangeRequestSnapshotMapper.toSnapshot(req);
        redis.opsForHash().putAll(rk, toHash(snap));

        Duration ttl = ttlFrom(req.getExpiresAt(), now);
        redis.expire(rk, ttl);

        // active pointer: only when active & not expired
        if (req.getUserId() != null && req.isActive() && !req.isExpired(now)) {
            String ak = EmailChangeRedisKeys.activeByUserKey(req.getUserId());
            redis.opsForValue().set(ak, String.valueOf(req.getId()), ttl);
        }
    }

    public Optional<EmailChangeRequest> findById(Long id, Instant now) {
        String rk = EmailChangeRedisKeys.requestKey(id);

        Map<Object, Object> raw = redis.opsForHash().entries(rk);
        if (raw == null || raw.isEmpty()) return Optional.empty();

        EmailChangeRequestSnapshot snap = fromHash(raw);
        EmailChangeRequest req = EmailChangeRequestSnapshotMapper.toDomain(snap);

        // guard for stale keys (TTL should handle it too)
        Instant expiresAt = req.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            redis.delete(rk);
            return Optional.empty();
        }

        return Optional.of(req);
    }

    public Optional<Long> findActiveIdByUserId(Long userId) {
        String ak = EmailChangeRedisKeys.activeByUserKey(userId);
        String v = redis.opsForValue().get(ak);

        if (v == null) return Optional.empty();

        String s = v.trim();
        if (s.isEmpty()) return Optional.empty();

        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            // pointer corrupted -> self heal
            redis.delete(ak);
            return Optional.empty();
        }
    }

    public void deleteActivePointer(Long userId) {
        String ak = EmailChangeRedisKeys.activeByUserKey(userId);
        redis.delete(ak);
    }

    public void deleteRequest(Long requestId) {
        String rk = EmailChangeRedisKeys.requestKey(requestId);
        redis.delete(rk);
    }

    public void deleteAllForRequest(Long requestId, Long userId) {
        redis.delete(EmailChangeRedisKeys.requestKey(requestId));
        if (userId != null) {
            redis.delete(EmailChangeRedisKeys.activeByUserKey(userId));
        }
    }

    // ---------- hash mapping ----------
    private static Map<String, String> toHash(EmailChangeRequestSnapshot s) {
        Map<String, String> m = new HashMap<>();

        m.put("schemaVersion", String.valueOf(s.schemaVersion()));

        m.put("id", nLong(s.id()));
        m.put("userId", nLong(s.userId()));

        m.put("oldEmail", nStr(s.oldEmail()));
        m.put("newEmail", nStr(s.newEmail()));

        m.put("secondFactorType", nStr(s.secondFactorType()));
        m.put("status", nStr(s.status()));

        m.put("expiresAtEpochMillis", nLong(s.expiresAtEpochMillis()));
        m.put("createdAtEpochMillis", nLong(s.createdAtEpochMillis()));
        m.put("updatedAtEpochMillis", nLong(s.updatedAtEpochMillis()));

        m.put("version", nLong(s.version()));

        return m;
    }

    private static EmailChangeRequestSnapshot fromHash(Map<Object, Object> raw) {
        return EmailChangeRequestSnapshot.builder()
                .schemaVersion(parseInt(get(raw, "schemaVersion"), 1))

                .id(parseLong(get(raw, "id")))
                .userId(parseLong(get(raw, "userId")))

                .oldEmail(blankToNull(get(raw, "oldEmail")))
                .newEmail(blankToNull(get(raw, "newEmail")))

                .secondFactorType(blankToNull(get(raw, "secondFactorType")))
                .status(blankToNull(get(raw, "status")))

                .expiresAtEpochMillis(parseLong(get(raw, "expiresAtEpochMillis")))
                .createdAtEpochMillis(parseLong(get(raw, "createdAtEpochMillis")))
                .updatedAtEpochMillis(parseLong(get(raw, "updatedAtEpochMillis")))

                .version(parseLong(get(raw, "version")))
                .build();
    }

    private static Duration ttlFrom(Instant expiresAt, Instant now) {
        if (expiresAt == null) return Duration.ofHours(24);
        Duration d = Duration.between(now, expiresAt);
        return (d.isNegative() || d.isZero()) ? Duration.ofSeconds(1) : d;
    }

    private static String get(Map<Object, Object> raw, String k) {
        Object v = raw.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private static int parseInt(String s, int def) {
        try { return (s == null || s.isBlank()) ? def : Integer.parseInt(s); }
        catch (Exception e) { return def; }
    }

    private static Long parseLong(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.valueOf(s); }
        catch (Exception e) { return null; }
    }

    private static String nStr(String s) { return s == null ? "" : s; }
    private static String nLong(Long v) { return v == null ? "" : String.valueOf(v); }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}