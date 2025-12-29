package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class JacksonPayloadReader {

    private final JsonMapper jsonMapper;

    public <T> T read(String json, Class<T> type) {
        try {
            return jsonMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse outbox payload into " + type.getSimpleName(), e);
        }
    }
}