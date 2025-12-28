package com.timeeconomy.notification.adapter.out.jpa.inbox.mapper;

import com.timeeconomy.notification.adapter.out.jpa.inbox.entity.ProcessedEventEntity;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;

public final class ProcessedEventMapper {
    private ProcessedEventMapper() {}

    public static ProcessedEventEntity toEntity(ProcessedEvent d) {
        ProcessedEventEntity e = new ProcessedEventEntity();
        e.setConsumerGroup(d.getConsumerGroup());
        e.setEventId(d.getEventId()); // UUID
        e.setEventType(d.getEventType());
        e.setTopic(d.getTopic());
        e.setKafkaPartition(d.getKafkaPartition());
        e.setKafkaOffset(d.getKafkaOffset());
        e.setProcessedAt(d.getProcessedAt()); 
        return e;
    }
}