package com.timeeconomy.notification.adapter.out.jpa.inbox.repository;

import com.timeeconomy.notification.adapter.out.jpa.inbox.entity.ProcessedEventEntity;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_events (
                consumer_group,
                event_id,
                event_type,
                topic,
                kafka_partition,
                kafka_offset,
                processed_at
            ) VALUES (
                :consumerGroup,
                :eventId,
                :eventType,
                :topic,
                :kafkaPartition,
                :kafkaOffset,
                :processedAt
            )
            ON CONFLICT (consumer_group, event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("consumerGroup") String consumerGroup,
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("topic") String topic,
            @Param("kafkaPartition") int kafkaPartition,
            @Param("kafkaOffset") long kafkaOffset,
            @Param("processedAt") Instant processedAt
    );
}
