package com.timeeconomy.auth.domain.outbox.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class OutboxEvent {

    private UUID id;

    private final String aggregateType;
    private final String aggregateId;

    private final String eventType;
    private final String payload;

    private OutboxStatus status;

    private final LocalDateTime occurredAt;
    private LocalDateTime availableAt;

    private int attempts;
    private String lastError;

    private String lockedBy;
    private LocalDateTime lockedAt;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;

    // ✅ keep constructor hidden
    private OutboxEvent(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxStatus status,
            LocalDateTime occurredAt,
            LocalDateTime availableAt,
            int attempts,
            String lastError,
            String lockedBy,
            LocalDateTime lockedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime sentAt
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
            LocalDateTime now
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

    // ✅ rehydrate (for JPA/Redis/DB mapping)
    public static OutboxEvent rehydrate(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxStatus status,
            LocalDateTime occurredAt,
            LocalDateTime availableAt,
            int attempts,
            String lastError,
            String lockedBy,
            LocalDateTime lockedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime sentAt
    ) {
        return new OutboxEvent(
                id, aggregateType, aggregateId, eventType, payload, status,
                occurredAt, availableAt, attempts, lastError, lockedBy, lockedAt,
                createdAt, updatedAt, sentAt
        );
    }

    // getters/setters as needed...
    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getAvailableAt() { return availableAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public String getLockedBy() { return lockedBy; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSentAt() { return sentAt; }

    public void setStatus(OutboxStatus status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}