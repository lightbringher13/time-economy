package com.timeeconomy.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "timeeconomy.outbox")
public record OutboxKafkaProperties(Map<String, String> topics) {}