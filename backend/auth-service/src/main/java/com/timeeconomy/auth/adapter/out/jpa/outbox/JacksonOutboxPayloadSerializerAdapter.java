package com.timeeconomy.auth.adapter.out.jpa.outbox;

import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class JacksonOutboxPayloadSerializerAdapter implements OutboxPayloadSerializerPort {

    private final JsonMapper jsonMapper;

    @Override
    public String serialize(Object payload) {
        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize outbox payload to JSON", e);
        }
    }
}