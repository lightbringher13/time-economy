package com.timeeconomy.notification.adapter.in.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleAuthUserRegisteredUseCase;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserRegisteredListener {

    private final HandleAuthUserRegisteredUseCase useCase;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(
        topics = "${topics.auth.user-registered}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, AuthUserRegisteredV1> record, Acknowledgment ack) {

        AuthUserRegisteredV1 event = record.value();

        var eventId = event.getEventId();
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

        log.info("Consumed eventType={} eventId={} topic={} key={} partition={} offset={} userId={}",
                ctx.eventType(), ctx.eventId(),
                record.topic(), record.key(), record.partition(), record.offset(),
                event.getUserId());
    }
}