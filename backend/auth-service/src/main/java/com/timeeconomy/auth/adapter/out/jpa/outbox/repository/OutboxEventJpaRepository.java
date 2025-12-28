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

    @Query(value = """
        UPDATE outbox_events e
          SET status     = 'PROCESSING',
              locked_by  = :workerId,
              locked_at  = :now,
              updated_at = :now,
              attempts   = e.attempts + 1
        WHERE e.id IN (
              SELECT id
                FROM outbox_events
                WHERE status IN ('PENDING','FAILED')
                  AND available_at <= CAST(:now AS timestamptz)
                  AND (
                        locked_at IS NULL
                        OR locked_at < (CAST(:now AS timestamptz) - make_interval(secs => :leaseSeconds))
                      )
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
               e.lastError = null,
               e.lockedBy = null,
               e.lockedAt = null
         where e.id = :id
           and e.lockedBy = :workerId
           and e.status = :processing
    """)
    int markSentOwned(
            @Param("id") UUID id,
            @Param("workerId") String workerId,
            @Param("processing") OutboxStatus processing,
            @Param("sent") OutboxStatus sent,
            @Param("sentAt") OffsetDateTime sentAt,
            @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEventEntity e
           set e.status = :failed,
               e.lastError = :error,
               e.updatedAt = :now,
               e.lockedBy = null,
               e.lockedAt = null,
               e.availableAt = :nextAvailableAt
         where e.id = :id
           and e.lockedBy = :workerId
           and e.status = :processing
    """)
    int markFailedOwned(
            @Param("id") UUID id,
            @Param("workerId") String workerId,
            @Param("processing") OutboxStatus processing,
            @Param("failed") OutboxStatus failed,
            @Param("error") String error,
            @Param("now") OffsetDateTime now,
            @Param("nextAvailableAt") OffsetDateTime nextAvailableAt
    );
}