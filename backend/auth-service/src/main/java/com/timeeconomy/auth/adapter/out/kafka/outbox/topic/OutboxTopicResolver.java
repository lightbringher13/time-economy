package com.timeeconomy.auth.adapter.out.kafka.outbox.topic;

public interface OutboxTopicResolver {
    String resolveTopic(String eventType);
}