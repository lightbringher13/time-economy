package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JacksonOutboxAvroMapperDispatcher implements OutboxAvroMapper {

    private final Map<String, EventTypeAvroMapper> mapperByEventType;

    public JacksonOutboxAvroMapperDispatcher(List<EventTypeAvroMapper> mappers) {
        this.mapperByEventType = mappers.stream()
                .collect(Collectors.toUnmodifiableMap(EventTypeAvroMapper::eventType, Function.identity()));
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        EventTypeAvroMapper mapper = mapperByEventType.get(event.getEventType());
        if (mapper == null) {
            throw new IllegalArgumentException("Unsupported eventType=" + event.getEventType());
        }
        return mapper.toAvro(event);
    }
}