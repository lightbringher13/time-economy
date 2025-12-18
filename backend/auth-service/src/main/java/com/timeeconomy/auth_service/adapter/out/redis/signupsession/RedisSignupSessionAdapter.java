package com.timeeconomy.auth_service.adapter.out.redis.signupsession;

import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth_service.domain.signupsession.port.out.SignupSessionStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

@Primary
@Component
@RequiredArgsConstructor
public class RedisSignupSessionAdapter implements SignupSessionStorePort {

    private final StringRedisTemplate redis;

    @Override
    public SignupSession save(SignupSession session) {
        UUID id = session.getId();
        String sk = SignupSessionRedisKeys.sessionKey(id);

        // old email for index maintenance
        String oldEmailNorm = null;
        Map<Object, Object> existing = redis.opsForHash().entries(sk);
        if (existing != null && !existing.isEmpty()) {
            oldEmailNorm = normalizeEmail(get(existing, "email"));
        }

        // write hash
        SignupSessionSnapshot snap = SignupSessionSnapshotMapper.toSnapshot(session);
        redis.opsForHash().putAll(sk, toHash(snap));

        // TTL
        redis.expire(sk, ttlFrom(session.getExpiresAt()));

        // update email index (createdAt DESC semantics)
        String newEmailNorm = normalizeEmail(session.getEmail());
        if (newEmailNorm != null) {
            String newIdx = SignupSessionRedisKeys.emailIndexKey(newEmailNorm);

            if (oldEmailNorm != null && !oldEmailNorm.equals(newEmailNorm)) {
                redis.opsForZSet().remove(SignupSessionRedisKeys.emailIndexKey(oldEmailNorm), id.toString());
            }

            double score = createdAtScore(session.getCreatedAt());
            redis.opsForZSet().add(newIdx, id.toString(), score);

            // optional: expire index key to avoid unbounded growth
            redis.expire(newIdx, Duration.ofDays(2));
        }

        return session;
    }

    @Override
    public Optional<SignupSession> findById(UUID id) {
        String sk = SignupSessionRedisKeys.sessionKey(id);

        Map<Object, Object> raw = redis.opsForHash().entries(sk);
        if (raw == null || raw.isEmpty()) return Optional.empty();

        SignupSessionSnapshot snap = SignupSessionSnapshotMapper.upgradeIfNeeded(fromHash(id, raw));
        SignupSession session = SignupSessionSnapshotMapper.toDomain(snap);

        // extra guard: if expiresAt passed, delete (TTL should also do it)
        if (session.getExpiresAt() != null && !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            redis.delete(sk);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public Optional<SignupSession> findLatestActiveByEmail(String email, LocalDateTime now) {
        String emailNorm = normalizeEmail(email);
        if (emailNorm == null) return Optional.empty();

        String idxKey = SignupSessionRedisKeys.emailIndexKey(emailNorm);

        // "top by createdAt desc" -> reverseRange
        int N = 25;
        Set<String> candidates = redis.opsForZSet().reverseRange(idxKey, 0, N - 1);
        if (candidates == null || candidates.isEmpty()) return Optional.empty();

        for (String sidStr : candidates) {
            UUID sid;
            try { sid = UUID.fromString(sidStr); }
            catch (Exception e) {
                redis.opsForZSet().remove(idxKey, sidStr);
                continue;
            }

            Optional<SignupSession> opt = findById(sid);
            if (opt.isEmpty()) {
                redis.opsForZSet().remove(idxKey, sidStr);
                continue;
            }

            SignupSession s = opt.get();

            // Match JPA predicate:
            // expiresAtAfter(now) AND state != COMPLETED
            if (s.getExpiresAt() == null || !s.getExpiresAt().isAfter(now)) {
                redis.opsForZSet().remove(idxKey, sidStr);
                continue;
            }
            if (s.getState() == SignupSessionState.COMPLETED) {
                redis.opsForZSet().remove(idxKey, sidStr);
                continue;
            }

            return Optional.of(s);
        }

        return Optional.empty();
    }

    // ------------------------
    // Snapshot <-> Hash mapping
    // ------------------------

    private static Map<String, String> toHash(SignupSessionSnapshot s) {
        Map<String, String> m = new HashMap<>();
        m.put("schemaVersion", String.valueOf(s.schemaVersion()));
        m.put("id", n(s.id()));

        m.put("email", n(s.email()));
        m.put("emailVerified", n(s.emailVerified()));
        m.put("phoneNumber", n(s.phoneNumber()));
        m.put("phoneVerified", n(s.phoneVerified()));

        m.put("name", n(s.name()));
        m.put("gender", n(s.gender()));
        m.put("birthDate", n(s.birthDate()));

        m.put("state", n(s.state()));

        m.put("createdAt", n(s.createdAt()));
        m.put("updatedAt", n(s.updatedAt()));
        m.put("expiresAt", n(s.expiresAt()));
        return m;
    }

    private static SignupSessionSnapshot fromHash(UUID id, Map<Object, Object> raw) {
        return SignupSessionSnapshot.builder()
                .schemaVersion(parseInt(get(raw, "schemaVersion"), 0))
                .id(get(raw, "id").isBlank() ? id.toString() : get(raw, "id"))
                .email(blankToNull(get(raw, "email")))
                .emailVerified(get(raw, "emailVerified"))
                .phoneNumber(blankToNull(get(raw, "phoneNumber")))
                .phoneVerified(get(raw, "phoneVerified"))
                .name(blankToNull(get(raw, "name")))
                .gender(blankToNull(get(raw, "gender")))
                .birthDate(blankToNull(get(raw, "birthDate")))
                .state(blankToNull(get(raw, "state")))
                .createdAt(blankToNull(get(raw, "createdAt")))
                .updatedAt(blankToNull(get(raw, "updatedAt")))
                .expiresAt(blankToNull(get(raw, "expiresAt")))
                .build();
    }

    private static Duration ttlFrom(LocalDateTime expiresAt) {
        if (expiresAt == null) return Duration.ofHours(24);
        Duration d = Duration.between(LocalDateTime.now(), expiresAt);
        return (d.isNegative() || d.isZero()) ? Duration.ofSeconds(1) : d;
    }

    private static double createdAtScore(LocalDateTime createdAt) {
        LocalDateTime t = (createdAt != null) ? createdAt : LocalDateTime.now();
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static String normalizeEmail(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase(Locale.ROOT);
        return e.isBlank() ? null : e;
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