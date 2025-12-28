package com.timeeconomy.auth.adapter.out.kafka.outbox.topic;

import java.util.Map;

public final class StaticOutboxTopicResolver implements OutboxTopicResolver {

    private final Map<String, String> mapping;

    public StaticOutboxTopicResolver(Map<String, String> mapping) {
        this.mapping = Map.copyOf(mapping);
    }

    @Override
    public String resolveTopic(String eventType) {
        String topic = mapping.get(eventType);
        if (topic == null) throw new IllegalArgumentException("No topic mapping for eventType=" + eventType);
        return topic;
    }
}