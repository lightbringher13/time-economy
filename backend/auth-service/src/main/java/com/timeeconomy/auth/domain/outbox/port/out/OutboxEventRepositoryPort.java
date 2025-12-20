package com.timeeconomy.auth.domain.outbox.port.out;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;
import java.util.List;

public interface OutboxEventRepositoryPort {
    OutboxEvent save(OutboxEvent event);
    Optional<OutboxEvent> findById(UUID id);
    List<OutboxEvent> claimBatch(String workerId, int limit, Duration lease, LocalDateTime now);
    void markSent(UUID id, LocalDateTime sentAt, LocalDateTime now);
    void markFailed(UUID id, String error, LocalDateTime now); // keep it PROCESSING or set FAILED
}