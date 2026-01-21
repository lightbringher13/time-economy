package com.timeeconomy.auth.adapter.out.redis.signupsession;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Primary
@Component
@RequiredArgsConstructor
public class RedisSignupSessionAdapter implements SignupSessionStorePort {

    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private final StringRedisTemplate redis;
    private final Clock clock;

    @Override
    public SignupSession createNew(Instant now) {
        // 1) build a new session with correct defaults
        SignupSession s = new SignupSession();

        UUID id = UUID.randomUUID();
        s.setId(id);

        s.setState(SignupSessionState.DRAFT);

        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.setExpiresAt(now.plus(SESSION_TTL));

        s.setEmailVerified(false);
        s.setEmailOtpPending(false);   // ✅ NEW

        s.setPhoneVerified(false);
        s.setPhoneOtpPending(false);   // ✅ NEW

        // other fields start null
        s.setEmail(null);
        s.setPhoneNumber(null);
        s.setName(null);
        s.setGender(null);
        s.setBirthDate(null);

        // 2) persist + TTL in one place (save() already sets expire())
        return save(s);
    }

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
        redis.expire(sk, ttlFrom(session.getExpiresAt(), clock));

        // update email index (createdAt DESC semantics)
        String newEmailNorm = normalizeEmail(session.getEmail());
        if (newEmailNorm != null) {
            String newIdx = SignupSessionRedisKeys.emailIndexKey(newEmailNorm);

            if (oldEmailNorm != null && !oldEmailNorm.equals(newEmailNorm)) {
                redis.opsForZSet().remove(SignupSessionRedisKeys.emailIndexKey(oldEmailNorm), id.toString());
            }

            double score = createdAtScore(session.getCreatedAt(), clock);
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

        SignupSessionSnapshot snap = fromHash(id, raw);
        SignupSession session = SignupSessionSnapshotMapper.toDomain(snap);

        Instant now = Instant.now(clock);
        if (session.getExpiresAt() != null && !session.getExpiresAt().isAfter(now)) {
            redis.delete(sk);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    @Override
    public Optional<SignupSession> findLatestActiveByEmail(String email, Instant now) {
        String emailNorm = normalizeEmail(email);
        if (emailNorm == null) return Optional.empty();

        String idxKey = SignupSessionRedisKeys.emailIndexKey(emailNorm);

        int N = 25;
        Set<String> candidates = redis.opsForZSet().reverseRange(idxKey, 0, N - 1);
        if (candidates == null || candidates.isEmpty()) return Optional.empty();

        for (String sidStr : candidates) {
            UUID sid;
            try {
                sid = UUID.fromString(sidStr);
            } catch (Exception e) {
                redis.opsForZSet().remove(idxKey, sidStr);
                continue;
            }

            Optional<SignupSession> opt = findById(sid);
            if (opt.isEmpty()) {
                redis.opsForZSet().remove(idxKey, sidStr);
                continue;
            }

            SignupSession s = opt.get();

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

        m.put("schemaVersion", Integer.toString(s.schemaVersion()));
        m.put("id", n(s.id()));

        m.put("email", n(s.email()));
        m.put("emailVerified", Boolean.toString(s.emailVerified()));
        m.put("emailOtpPending", Boolean.toString(s.emailOtpPending()));   // ✅ NEW

        m.put("phoneNumber", n(s.phoneNumber()));
        m.put("phoneVerified", Boolean.toString(s.phoneVerified()));
        m.put("phoneOtpPending", Boolean.toString(s.phoneOtpPending()));   // ✅ NEW

        m.put("name", n(s.name()));
        m.put("gender", n(s.gender()));
        m.put("birthDateEpochDays", nInt(s.birthDateEpochDays()));

        m.put("state", n(s.state()));

        m.put("createdAtEpochMillis", nLong(s.createdAtEpochMillis()));
        m.put("updatedAtEpochMillis", nLong(s.updatedAtEpochMillis()));
        m.put("expiresAtEpochMillis", nLong(s.expiresAtEpochMillis()));

        return m;
    }

    private static SignupSessionSnapshot fromHash(UUID id, Map<Object, Object> raw) {
        return SignupSessionSnapshot.builder()
                .schemaVersion(parseInt(get(raw, "schemaVersion"), 1))
                .id(hasText(get(raw, "id")) ? get(raw, "id") : id.toString())

                .email(blankToNull(get(raw, "email")))
                .emailVerified(parseBool(get(raw, "emailVerified"), false))
                .emailOtpPending(parseBool(get(raw, "emailOtpPending"), false))   // ✅ NEW (defaults false)

                .phoneNumber(blankToNull(get(raw, "phoneNumber")))
                .phoneVerified(parseBool(get(raw, "phoneVerified"), false))
                .phoneOtpPending(parseBool(get(raw, "phoneOtpPending"), false))   // ✅ NEW (defaults false)

                .name(blankToNull(get(raw, "name")))
                .gender(blankToNull(get(raw, "gender")))
                .birthDateEpochDays(parseIntObj(get(raw, "birthDateEpochDays")))

                .state(blankToNull(get(raw, "state")))

                .createdAtEpochMillis(parseLongObj(get(raw, "createdAtEpochMillis")))
                .updatedAtEpochMillis(parseLongObj(get(raw, "updatedAtEpochMillis")))
                .expiresAtEpochMillis(parseLongObj(get(raw, "expiresAtEpochMillis")))
                .build();
    }

    private static Duration ttlFrom(Instant expiresAt, Clock clock) {
        if (expiresAt == null) return Duration.ofHours(24);

        Instant now = Instant.now(clock);
        Duration d = Duration.between(now, expiresAt);
        return (d.isNegative() || d.isZero()) ? Duration.ofSeconds(1) : d;
    }

    private static double createdAtScore(Instant createdAt, Clock clock) {
        Instant t = (createdAt != null) ? createdAt : Instant.now(clock);
        return t.getEpochSecond();
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

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static Integer parseIntObj(String s) {
        if (!hasText(s)) return null;
        try { return Integer.valueOf(s); } catch (Exception e) { return null; }
    }

    private static Long parseLongObj(String s) {
        if (!hasText(s)) return null;
        try { return Long.valueOf(s); } catch (Exception e) { return null; }
    }

    private static boolean parseBool(String s, boolean def) {
        if (!hasText(s)) return def;
        return Boolean.parseBoolean(s);
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String nLong(Long v) { return v == null ? "" : v.toString(); }
    private static String nInt(Integer v) { return v == null ? "" : v.toString(); }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}