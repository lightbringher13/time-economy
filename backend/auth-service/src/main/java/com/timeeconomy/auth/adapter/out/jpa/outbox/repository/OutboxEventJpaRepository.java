package com.timeeconomy.auth.adapter.out.jpa.outbox.repository;

import com.timeeconomy.auth.adapter.out.jpa.outbox.entity.OutboxEventEntity;
import com.timeeconomy.auth.domain.outbox.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Claim PENDING events in a safe way:
     * - only rows that are ready (available_at <= now)
     * - lock lease logic via locked_at
     * - returns claimed rows (Postgres supports RETURNING)
     *
     * IMPORTANT: this should be a native query.
     */
    @Query(value = """
        UPDATE outbox_events e
           SET status    = 'PROCESSING',
               locked_by = :workerId,
               locked_at = :now,
               updated_at = :now
         WHERE e.id IN (
               SELECT id
                 FROM outbox_events
                WHERE status = 'PENDING'
                  AND available_at <= :now
                  AND (locked_at IS NULL OR locked_at < (:now - (:leaseSeconds || ' seconds')::interval))
                ORDER BY created_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
           )
        RETURNING *
        """, nativeQuery = true)
    List<OutboxEventEntity> claimBatch(
            @Param("workerId") String workerId,
            @Param("limit") int limit,
            @Param("leaseSeconds") long leaseSeconds,
            @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEventEntity e
           set e.status = :sent,
               e.sentAt = :sentAt,
               e.updatedAt = :now,
               e.lastError = null
         where e.id = :id
    """)
    int markSent(
            @Param("id") UUID id,
            @Param("sent") OutboxStatus sent,
            @Param("sentAt") OffsetDateTime sentAt,
            @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEventEntity e
           set e.status = :failed,
               e.attempts = e.attempts + 1,
               e.lastError = :error,
               e.updatedAt = :now
         where e.id = :id
    """)
    int markFailed(
            @Param("id") UUID id,
            @Param("failed") OutboxStatus failed,
            @Param("error") String error,
            @Param("now") OffsetDateTime now
    );
}