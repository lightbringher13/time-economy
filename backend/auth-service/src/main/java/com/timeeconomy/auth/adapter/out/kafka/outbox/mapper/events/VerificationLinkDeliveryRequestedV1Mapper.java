package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.events;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.EventTypeAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.JacksonPayloadReader;
import com.timeeconomy.auth.domain.verification.model.payload.VerificationLinkDeliveryRequestedPayload;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.contracts.auth.v1.VerificationLinkDeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerificationLinkDeliveryRequestedV1Mapper implements EventTypeAvroMapper {

    private final JacksonPayloadReader reader;

    @Override
    public String eventType() {
        return "VerificationLinkDeliveryRequested.v1";
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        VerificationLinkDeliveryRequestedPayload p =
                reader.read(event.getPayload(), VerificationLinkDeliveryRequestedPayload.class);

        return VerificationLinkDeliveryRequestedV1.newBuilder()
                .setEventId(event.getId().toString())
                .setOccurredAtEpochMillis(event.getOccurredAt().toEpochMilli())                // ✅ Instant in your codegen
                .setVerificationChallengeId(p.verificationChallengeId().toString()) // ✅ UUID
                .setPurpose(p.purpose())
                .setChannel(p.channel())
                .setSubjectType(p.subjectType())
                .setSubjectId(p.subjectId())
                .setDestinationNorm(p.destinationNorm())
                .setTtlSeconds(p.ttlSeconds())
                .build();
    }
}