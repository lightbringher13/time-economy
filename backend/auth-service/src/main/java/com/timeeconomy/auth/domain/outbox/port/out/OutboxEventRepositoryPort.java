package com.timeeconomy.auth.domain.outbox.port.out;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepositoryPort {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);

    /**
     * Claim a batch of events for processing.
     *
     * @param workerId unique worker identifier
     * @param limit max number of rows to claim
     * @param lease lock lease duration (used by DB claim query)
     * @param now current time (Instant)
     */
    List<OutboxEvent> claimBatch(String workerId, int limit, Duration lease, Instant now);

    /**
     * Mark an event as SENT.
     *
     * @return updated row count (0 means: not owned / not processing / already changed)
     */
    int markSent(UUID id, String workerId, Instant sentAt, Instant now);

    /**
     * Mark an event as FAILED and schedule next retry time.
     *
     * @return updated row count (0 means: not owned / not processing / already changed)
     */
    int markFailed(UUID id, String workerId, int attempts, String error, Instant now);
}