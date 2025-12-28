package com.timeeconomy.notification.domain.inbox.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ProcessedEvent {

    private Long id;

    private final String consumerGroup;

    // ✅ outbox UUID
    private final UUID eventId;

    private final String eventType;

    private final String topic;

    // ✅ match DB column meaning (kafka_partition / kafka_offset)
    private final int kafkaPartition;
    private final long kafkaOffset;

    private final Instant processedAt;

    private ProcessedEvent(
            Long id,
            String consumerGroup,
            UUID eventId,
            String eventType,
            String topic,
            int kafkaPartition,
            long kafkaOffset,
            Instant processedAt
    ) {
        this.id = id;
        this.consumerGroup = Objects.requireNonNull(consumerGroup, "consumerGroup");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.kafkaPartition = kafkaPartition;
        this.kafkaOffset = kafkaOffset;
        this.processedAt = processedAt != null ? processedAt : Instant.now();
    }

    public static ProcessedEvent newProcessed(
            String consumerGroup,
            UUID eventId,
            String eventType,
            String topic,
            int kafkaPartition,
            long kafkaOffset,
            Instant now
    ) {
        return new ProcessedEvent(
                null,
                consumerGroup,
                eventId,
                eventType,
                topic,
                kafkaPartition,
                kafkaOffset,
                now
        );
    }

    // getters
    public Long getId() { return id; }
    public String getConsumerGroup() { return consumerGroup; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public int getKafkaPartition() { return kafkaPartition; }
    public long getKafkaOffset() { return kafkaOffset; }
    public Instant getProcessedAt() { return processedAt; }
}