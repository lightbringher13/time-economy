package com.timeeconomy.notification.adapter.out.jpa.inbox.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_processed_events", columnNames = {"consumer_group", "event_id"})
        }
)
public class ProcessedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_group", nullable = false, length = 200)
    private String consumerGroup;

    // ✅ DDL: event_id UUID NOT NULL
    @Column(name = "event_id", nullable = false, columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 300)
    private String topic;

    // ✅ DDL: kafka_partition INT NOT NULL
    @Column(name = "kafka_partition", nullable = false)
    private int kafkaPartition;

    // ✅ DDL: kafka_offset BIGINT NOT NULL
    @Column(name = "kafka_offset", nullable = false)
    private long kafkaOffset;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEventEntity() {}

    // getters/setters
    public Long getId() { return id; }

    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getKafkaPartition() { return kafkaPartition; }
    public void setKafkaPartition(int kafkaPartition) { this.kafkaPartition = kafkaPartition; }

    public long getKafkaOffset() { return kafkaOffset; }
    public void setKafkaOffset(long kafkaOffset) { this.kafkaOffset = kafkaOffset; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}