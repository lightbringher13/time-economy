package com.timeeconomy.auth.adapter.out.jpa.outbox;

import com.timeeconomy.auth.adapter.out.jpa.outbox.entity.OutboxEventEntity;
import com.timeeconomy.auth.adapter.out.jpa.outbox.mapper.OutboxEventMapper;
import com.timeeconomy.auth.adapter.out.jpa.outbox.repository.OutboxEventJpaRepository;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.model.OutboxStatus;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventJpaAdapter implements OutboxEventRepositoryPort {

    private final OutboxEventJpaRepository repo;
    private final OutboxEventMapper mapper;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity saved = repo.save(mapper.toEntity(event));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public List<OutboxEvent> claimBatch(String workerId, int limit, Duration lease, LocalDateTime now) {
        var rows = repo.claimBatch(workerId, limit, lease.getSeconds(), now.atOffset(ZoneOffset.UTC));
        return rows.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public int markSent(UUID id, String workerId, LocalDateTime sentAt, LocalDateTime now) {
        return repo.markSentOwned(
                id,
                workerId,
                OutboxStatus.PROCESSING,
                OutboxStatus.SENT,
                sentAt.atOffset(ZoneOffset.UTC),
                now.atOffset(ZoneOffset.UTC)
        );
    }

    @Override
    @Transactional
    public int markFailed(UUID id, String workerId, int attempts, String error, LocalDateTime now) {
        String safeError = error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error);

        long delaySeconds = (attempts <= 0) ? 1L : Math.min((1L << Math.min(attempts, 10)), 300L);
        LocalDateTime nextAvailableAt = now.plusSeconds(delaySeconds);

        return repo.markFailedOwned(
                id,
                workerId,
                OutboxStatus.PROCESSING,
                OutboxStatus.FAILED,
                safeError,
                now.atOffset(ZoneOffset.UTC),
                nextAvailableAt.atOffset(ZoneOffset.UTC)
        );
    }
}