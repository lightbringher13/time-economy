package com.timeeconomy.auth.adapter.out.changeemail;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.jpa.changeemail.EmailChangeRequestJpaAdapter;
import com.timeeconomy.auth.adapter.out.redis.changeemail.EmailChangeRequestRedisStore;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;

import java.time.Instant;
import java.util.Optional;
import java.time.Clock;

@Primary
@Component
@RequiredArgsConstructor
public class EmailChangeRequestHybridAdapter implements EmailChangeRequestRepositoryPort {

    private final EmailChangeRequestJpaAdapter jpa; // concrete injection avoids self-injection
    private final EmailChangeRequestRedisStore redisStore;

    private final Clock clock;

    @Override
    public EmailChangeRequest save(EmailChangeRequest request) {
        Instant now = Instant.now(clock);

        // If id is null, we MUST create DB row first (Long id is DB-generated)
        EmailChangeRequest persisted = (request.getId() == null) ? jpa.save(request) : request;

        // Terminal states: persist to DB and ensure Redis is removed (no need to upsert Redis)
        if (isTerminal(persisted.getStatus())) {
            EmailChangeRequest savedTerminal = jpa.save(persisted);
            redisStore.deleteAllForRequest(savedTerminal.getId(), savedTerminal.getUserId());
            return savedTerminal;
        }

        // Active/in-flight states: keep in Redis (cache + TTL)
        redisStore.upsert(persisted, now);
        return persisted;
    }

    @Override
    public Optional<EmailChangeRequest> findActiveByUserId(Long userId) {
        Instant now = Instant.now(clock);

        // 1) Try Redis pointer -> Redis hash
        Optional<Long> idOpt = redisStore.findActiveIdByUserId(userId);

        Optional<EmailChangeRequest> fromRedis = idOpt
                .flatMap(id -> redisStore.findById(id, now))
                .filter(r -> userId.equals(r.getUserId()))
                .filter(r -> r.isActive() && !r.isExpired(now));

        if (fromRedis.isPresent()) {
            return fromRedis;
        }

        // ✅ pointer existed, but request missing/invalid/expired -> clean it up
        if (idOpt.isPresent()) {
            redisStore.deleteActivePointer(userId);

            // optional: also delete stale hash if you want aggressive cleanup
            // redisStore.deleteRequest(idOpt.get());
        }

        // 2) Fallback to DB
        Optional<EmailChangeRequest> fromDb = jpa.findActiveByUserId(userId)
                .filter(r -> r.isActive() && !r.isExpired(now));

        // 3) Read-through cache: re-hydrate Redis so next call is fast
        fromDb.ifPresent(r -> redisStore.upsert(r, now));

        return fromDb;
    }

    @Override
    public Optional<EmailChangeRequest> findByIdAndUserId(Long id, Long userId) {
        Instant now = Instant.now(clock);

        Optional<EmailChangeRequest> raw = redisStore.findById(id, now);

        if (raw.isPresent()) {
            EmailChangeRequest r = raw.get();

            // Redis corruption / wrong mapping: same requestId but different user
            if (!userId.equals(r.getUserId())) {
                redisStore.deleteRequest(id); // self-heal
                return Optional.empty();
            }

            return Optional.of(r);
        }

        // Fallback to DB (history / durability)
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public void delete(EmailChangeRequest request) {
        // Keeping your current semantics (hard delete).
        // In BigCom, you'd typically markCanceled + save() instead.
        if (request.getId() != null) {
            redisStore.deleteAllForRequest(request.getId(), request.getUserId());
        }
        jpa.delete(request);
    }

    @Override
    public Optional<EmailChangeRequest> findByUserIdAndStatus(Long userId, EmailChangeStatus status) {
        // history query -> DB
        return jpa.findByUserIdAndStatus(userId, status);
    }

    private static boolean isTerminal(EmailChangeStatus s) {
        return s == EmailChangeStatus.COMPLETED
                || s == EmailChangeStatus.CANCELED
                || s == EmailChangeStatus.EXPIRED;
    }

    @Override
    public Optional<EmailChangeRequest> findById(Long id) {
        Instant now = Instant.now(clock);

        // 1) Redis first (fast path for in-flight)
        Optional<EmailChangeRequest> fromRedis = redisStore.findById(id, now);
        if (fromRedis.isPresent()) return fromRedis;

        // 2) DB fallback (history / durability)
        Optional<EmailChangeRequest> fromDb = jpa.findById(id);

        // 3) Optional: read-through cache only if it's active (don’t re-cache terminal history)
        fromDb.filter(r -> r.isActive() && !r.isExpired(now))
            .ifPresent(r -> redisStore.upsert(r, now));

        return fromDb;
    }
}