package com.timeeconomy.auth.domain.outbox.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OutboxEvent {

    private UUID id;

    private final String aggregateType;
    private final String aggregateId;

    private final String eventType;
    private final String payload;

    private OutboxStatus status;

    // ✅ Instant-friendly timestamps
    private final Instant occurredAt;
    private Instant availableAt;

    private int attempts;
    private String lastError;

    private String lockedBy;
    private Instant lockedAt;

    private final Instant createdAt;
    private Instant updatedAt;
    private Instant sentAt;

    // ✅ keep constructor hidden
    private OutboxEvent(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxStatus status,
            Instant occurredAt,
            Instant availableAt,
            int attempts,
            String lastError,
            String lockedBy,
            Instant lockedAt,
            Instant createdAt,
            Instant updatedAt,
            Instant sentAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.status = Objects.requireNonNull(status, "status");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
        this.attempts = attempts;
        this.lastError = lastError;
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.sentAt = sentAt;
    }

    // ✅ new event (domain creation)
    public static OutboxEvent newPending(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson,
            Instant now
    ) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payloadJson,
                OutboxStatus.PENDING,
                now,
                now,
                0,
                null,
                null,
                null,
                now,
                now,
                null
        );
    }

    // ✅ rehydrate (for DB/JPA mapping)
    public static OutboxEvent rehydrate(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxStatus status,
            Instant occurredAt,
            Instant availableAt,
            int attempts,
            String lastError,
            String lockedBy,
            Instant lockedAt,
            Instant createdAt,
            Instant updatedAt,
            Instant sentAt
    ) {
        return new OutboxEvent(
                id, aggregateType, aggregateId, eventType, payload, status,
                occurredAt, availableAt, attempts, lastError, lockedBy, lockedAt,
                createdAt, updatedAt, sentAt
        );
    }

    // ---------------------------------------------------------
    // Getters
    // ---------------------------------------------------------
    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }

    public Instant getOccurredAt() { return occurredAt; }
    public Instant getAvailableAt() { return availableAt; }

    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }

    public String getLockedBy() { return lockedBy; }
    public Instant getLockedAt() { return lockedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSentAt() { return sentAt; }

    // ---------------------------------------------------------
    // Setters / state transitions
    // ---------------------------------------------------------
    public void setStatus(OutboxStatus status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }

    public void setAvailableAt(Instant availableAt) { this.availableAt = availableAt; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}