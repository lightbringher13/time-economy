package com.timeeconomy.auth.adapter.out.jpa.outbox.mapper;

import com.timeeconomy.auth.adapter.out.jpa.outbox.entity.OutboxEventEntity;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEventEntity toEntity(OutboxEvent d) {
        if (d == null) return null;

        OutboxEventEntity e = new OutboxEventEntity();
        e.setId(d.getId());
        e.setAggregateType(d.getAggregateType());
        e.setAggregateId(d.getAggregateId());
        e.setEventType(d.getEventType());
        e.setPayload(d.getPayload());
        e.setStatus(d.getStatus());

        // ✅ Instant 그대로
        e.setOccurredAt(d.getOccurredAt());
        e.setAvailableAt(d.getAvailableAt());
        e.setAttempts(d.getAttempts());
        e.setLastError(d.getLastError());
        e.setLockedBy(d.getLockedBy());
        e.setLockedAt(d.getLockedAt());

        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setSentAt(d.getSentAt());

        return e;
    }

    public OutboxEvent toDomain(OutboxEventEntity e) {
        if (e == null) return null;

        return OutboxEvent.rehydrate(
                e.getId(),
                e.getAggregateType(),
                e.getAggregateId(),
                e.getEventType(),
                e.getPayload(),
                e.getStatus(),
                e.getOccurredAt(),
                e.getAvailableAt(),
                e.getAttempts(),
                e.getLastError(),
                e.getLockedBy(),
                e.getLockedAt(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getSentAt()
        );
    }
}