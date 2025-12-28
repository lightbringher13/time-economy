package com.timeeconomy.auth.domain.outbox.port.out;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepositoryPort {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);

    List<OutboxEvent> claimBatch(String workerId, int limit, Duration lease, LocalDateTime now);

    /** returns updated row count (0 means: not owned / not processing / already changed) */
    int markSent(UUID id, String workerId, LocalDateTime sentAt, LocalDateTime now);

    /** returns updated row count (0 means: not owned / not processing / already changed) */
    int markFailed(UUID id, String workerId, int attempts, String error, LocalDateTime now);
}