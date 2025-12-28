package com.timeeconomy.notification.adapter.in.kafka.dto;

import org.apache.kafka.common.header.Headers;

import java.util.UUID;

public record ConsumerContext(
        String consumerGroup,
        String topic,
        int partition,
        long offset,
        long timestamp,
        String key,
        Headers headers,
        UUID eventId,
        String eventType
) {}