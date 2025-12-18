package com.timeeconomy.auth.adapter.out.redis.changeemail;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailChangeRequestRedisStore {

    private final StringRedisTemplate redis;

    public void upsert(EmailChangeRequest req, LocalDateTime now) {
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
            redis.opsForValue().set(ak, req.getId().toString(), ttl);
        }
    }

    public Optional<EmailChangeRequest> findById(Long id, LocalDateTime now) {
        String rk = EmailChangeRedisKeys.requestKey(id);

        Map<Object, Object> raw = redis.opsForHash().entries(rk);
        if (raw == null || raw.isEmpty()) return Optional.empty();

        EmailChangeRequestSnapshot snap = EmailChangeRequestSnapshotMapper.upgradeIfNeeded(fromHash(raw));
        EmailChangeRequest req = EmailChangeRequestSnapshotMapper.toDomain(snap);

        // guard for stale keys
        if (req.getExpiresAt() != null && !req.getExpiresAt().isAfter(now)) {
            redis.delete(rk);
            return Optional.empty();
        }

        return Optional.of(req);
    }

    public Optional<Long> findActiveIdByUserId(Long userId) {
        String ak = EmailChangeRedisKeys.activeByUserKey(userId);
        String v = redis.opsForValue().get(ak);
        try {
            return (v == null || v.isBlank()) ? Optional.empty() : Optional.of(Long.valueOf(v));
        } catch (Exception e) {
            redis.delete(ak);
            return Optional.empty();
        }
    }

    public void deleteAllForRequest(Long requestId, Long userId) {
        redis.delete(EmailChangeRedisKeys.requestKey(requestId));
        if (userId != null) {
            // safe delete; if you want strict delete-only-when-matches, you can compare value first
            redis.delete(EmailChangeRedisKeys.activeByUserKey(userId));
        }
    }

    // ---------- hash mapping ----------
    private static Map<String, String> toHash(EmailChangeRequestSnapshot s) {
        Map<String, String> m = new HashMap<>();
        m.put("schemaVersion", String.valueOf(s.schemaVersion()));
        m.put("id", n(s.id()));
        m.put("userId", n(s.userId()));
        m.put("oldEmail", n(s.oldEmail()));
        m.put("newEmail", n(s.newEmail()));
        m.put("secondFactorType", n(s.secondFactorType()));
        m.put("status", n(s.status()));
        m.put("expiresAt", n(s.expiresAt()));
        m.put("createdAt", n(s.createdAt()));
        m.put("updatedAt", n(s.updatedAt()));
        m.put("version", n(s.version()));
        return m;
    }

    private static EmailChangeRequestSnapshot fromHash(Map<Object, Object> raw) {
        return EmailChangeRequestSnapshot.builder()
                .schemaVersion(parseInt(get(raw, "schemaVersion"), 0))
                .id(blankToNull(get(raw, "id")))
                .userId(blankToNull(get(raw, "userId")))
                .oldEmail(blankToNull(get(raw, "oldEmail")))
                .newEmail(blankToNull(get(raw, "newEmail")))
                .secondFactorType(blankToNull(get(raw, "secondFactorType")))
                .status(blankToNull(get(raw, "status")))
                .expiresAt(blankToNull(get(raw, "expiresAt")))
                .createdAt(blankToNull(get(raw, "createdAt")))
                .updatedAt(blankToNull(get(raw, "updatedAt")))
                .version(blankToNull(get(raw, "version")))
                .build();
    }

    private static Duration ttlFrom(LocalDateTime expiresAt, LocalDateTime now) {
        if (expiresAt == null) return Duration.ofHours(24);
        Duration d = Duration.between(now, expiresAt);
        return (d.isNegative() || d.isZero()) ? Duration.ofSeconds(1) : d;
    }

    private static String get(Map<Object, Object> raw, String k) {
        Object v = raw.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private static int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private static String n(String s) { return s == null ? "" : s; }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}