package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import org.apache.avro.specific.SpecificRecord;

public interface EventTypeAvroMapper {
    String eventType();                 // e.g. "EmailChangeCommitted.v1"
    SpecificRecord toAvro(OutboxEvent event);
}