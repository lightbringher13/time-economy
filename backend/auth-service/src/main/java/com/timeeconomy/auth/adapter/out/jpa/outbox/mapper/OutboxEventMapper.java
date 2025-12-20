package com.timeeconomy.auth.adapter.out.jpa.outbox.mapper;

import com.timeeconomy.auth.adapter.out.jpa.outbox.entity.OutboxEventEntity;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class OutboxEventMapper {

    public OutboxEventEntity toEntity(OutboxEvent d) {
        OutboxEventEntity e = new OutboxEventEntity();
        e.setId(d.getId());
        e.setAggregateType(d.getAggregateType());
        e.setAggregateId(d.getAggregateId());
        e.setEventType(d.getEventType());
        e.setPayload(d.getPayload());
        e.setStatus(d.getStatus());

        e.setOccurredAt(toOffset(d.getOccurredAt()));
        e.setAvailableAt(toOffset(d.getAvailableAt()));
        e.setAttempts(d.getAttempts());
        e.setLastError(d.getLastError());
        e.setLockedBy(d.getLockedBy());
        e.setLockedAt(toOffsetNullable(d.getLockedAt()));

        e.setCreatedAt(toOffset(d.getCreatedAt()));
        e.setUpdatedAt(toOffset(d.getUpdatedAt()));
        e.setSentAt(toOffsetNullable(d.getSentAt()));
        return e;
    }

    public OutboxEvent toDomain(OutboxEventEntity e) {
        return OutboxEvent.rehydrate(
                e.getId(),
                e.getAggregateType(),
                e.getAggregateId(),
                e.getEventType(),
                e.getPayload(),
                e.getStatus(),
                toLocal(e.getOccurredAt()),
                toLocal(e.getAvailableAt()),
                e.getAttempts(),
                e.getLastError(),
                e.getLockedBy(),
                toLocalNullable(e.getLockedAt()),
                toLocal(e.getCreatedAt()),
                toLocal(e.getUpdatedAt()),
                toLocalNullable(e.getSentAt())
        );
    }

    private static OffsetDateTime toOffset(LocalDateTime t) {
        return t.atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime toOffsetNullable(LocalDateTime t) {
        return t == null ? null : t.atOffset(ZoneOffset.UTC);
    }

    private static LocalDateTime toLocal(OffsetDateTime t) {
        return t.toLocalDateTime();
    }

    private static LocalDateTime toLocalNullable(OffsetDateTime t) {
        return t == null ? null : t.toLocalDateTime();
    }
}