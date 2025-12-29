package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.events;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.EventTypeAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.JacksonPayloadReader;
import com.timeeconomy.auth.domain.verification.model.payload.VerificationOtpDeliveryRequestedPayload;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.contracts.auth.v1.VerificationOtpDeliveryRequestedV1;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VerificationOtpDeliveryRequestedV1Mapper implements EventTypeAvroMapper {

    private final JacksonPayloadReader reader;

    @Override
    public String eventType() {
        return "VerificationOtpDeliveryRequested.v1";
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        VerificationOtpDeliveryRequestedPayload p =
                reader.read(event.getPayload(), VerificationOtpDeliveryRequestedPayload.class);

        Instant occurred = event.getOccurredAt().atZone(ZoneOffset.UTC).toInstant();

        return VerificationOtpDeliveryRequestedV1.newBuilder()
                .setEventId(UUID.fromString(event.getId().toString()))
                .setOccurredAtEpochMillis(occurred)                 // ✅ Instant in your codegen
                .setVerificationChallengeId(p.verificationChallengeId()) // ✅ UUID
                .setPurpose(p.purpose())
                .setChannel(p.channel())
                .setSubjectType(p.subjectType())
                .setSubjectId(p.subjectId())
                .setDestinationNorm(p.destinationNorm())
                .setTtlSeconds(p.ttlSeconds())
                .build();
    }
}