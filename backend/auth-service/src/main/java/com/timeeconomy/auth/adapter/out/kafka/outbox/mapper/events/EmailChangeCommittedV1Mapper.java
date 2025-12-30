package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.events;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.EventTypeAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.JacksonPayloadReader;
import com.timeeconomy.auth.domain.changeemail.model.payload.EmailChangeCommittedPayload;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailChangeCommittedV1Mapper implements EventTypeAvroMapper {

    private final JacksonPayloadReader reader;

    @Override
    public String eventType() {
        return "EmailChangeCommitted.v1";
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        EmailChangeCommittedPayload p = reader.read(event.getPayload(), EmailChangeCommittedPayload.class);

        return EmailChangeCommittedV1.newBuilder()
                .setEventId(UUID.fromString(event.getId().toString()))
                .setOccurredAtEpochMillis(event.getOccurredAt())
                .setUserId(p.userId())
                .setOldEmail(p.oldEmail())
                .setNewEmail(p.newEmail())
                .build();
    }
}