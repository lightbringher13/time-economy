package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import org.apache.avro.specific.SpecificRecord;

public interface OutboxAvroMapper {
    SpecificRecord toAvro(OutboxEvent event);
}