package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.events;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.EventTypeAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.JacksonPayloadReader;
import com.timeeconomy.auth.domain.auth.model.payload.AuthUserRegisteredPayload;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.contracts.auth.v2.AuthUserRegisteredV2;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserRegisteredV2Mapper implements EventTypeAvroMapper {

    private final JacksonPayloadReader reader;

    @Override
    public String eventType() {
        return "AuthUserRegistered.v2";
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        AuthUserRegisteredPayload p = reader.read(event.getPayload(), AuthUserRegisteredPayload.class);

        return AuthUserRegisteredV2.newBuilder()
                // ⚠️ recommended: eventId is uuid-string logicalType → String
                .setEventId(event.getId())
                // timestamp-millis → long
                .setOccurredAtEpochMillis(event.getOccurredAt())
                .setUserId(p.userId())
                .setSignupSessionId(p.signupSessionId())
                .build();
    }
}