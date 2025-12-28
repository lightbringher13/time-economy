package com.timeeconomy.auth.config;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.JacksonOutboxAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.OutboxAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.topic.OutboxTopicResolver;
import com.timeeconomy.auth.adapter.out.kafka.outbox.topic.StaticOutboxTopicResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;
@Configuration
@EnableConfigurationProperties(OutboxKafkaProperties.class)
public class OutboxKafkaBeansConfig {

    @Bean
    public OutboxTopicResolver outboxTopicResolver(OutboxKafkaProperties props) {
        return new StaticOutboxTopicResolver(props.topics());
    }

    @Bean
    public OutboxAvroMapper outboxAvroMapper(JsonMapper jsonMapper) {
        return new JacksonOutboxAvroMapper(jsonMapper);
    }
}