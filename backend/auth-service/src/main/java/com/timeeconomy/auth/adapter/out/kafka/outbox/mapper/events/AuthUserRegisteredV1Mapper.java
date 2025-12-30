package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.events;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.EventTypeAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.JacksonPayloadReader;
import com.timeeconomy.auth.domain.auth.model.payload.AuthUserRegisteredPayload;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthUserRegisteredV1Mapper implements EventTypeAvroMapper {

    private final JacksonPayloadReader reader;

    @Override
    public String eventType() {
        return "AuthUserRegistered.v1";
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        AuthUserRegisteredPayload p = reader.read(event.getPayload(), AuthUserRegisteredPayload.class);

        return AuthUserRegisteredV1.newBuilder()
                // ⚠️ recommended: eventId is uuid-string logicalType → String
                .setEventId(UUID.fromString(event.getId().toString()))
                // timestamp-millis → long
                .setOccurredAtEpochMillis(event.getOccurredAt())
                .setUserId(p.userId())
                .setEmail(p.email())
                .setPhoneNumber(p.phoneNumber())
                .setName(p.name())
                .setGender(p.gender())
                .setBirthDateEpochDays(p.birthDate())
                .setSignupSessionId(p.signupSessionId())
                .build();
    }
}