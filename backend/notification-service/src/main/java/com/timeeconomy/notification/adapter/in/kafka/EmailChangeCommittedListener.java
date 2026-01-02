package com.timeeconomy.notification.adapter.in.kafka;

import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleEmailChangeCommittedUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChangeCommittedListener {

    private final HandleEmailChangeCommittedUseCase useCase;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(
        topics = "${topics.auth.email-changed}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, EmailChangeCommittedV1> record, Acknowledgment ack) {

        EmailChangeCommittedV1 event = record.value();

        var eventId = UUID.fromString(event.getEventId());
        var eventType = KafkaHeaderUtil.str(record.headers(), "event_type");

        ConsumerContext ctx = new ConsumerContext(
                consumerGroup,
                record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp(),
                record.key(),
                record.headers(),
                eventId,
                eventType
        );

        useCase.handle(event, ctx);

        ack.acknowledge();

        log.info("Consumed topic={} key={} partition={} offset={} userId={}",
                record.topic(), record.key(), record.partition(), record.offset(),
                event.getUserId());
    }
}