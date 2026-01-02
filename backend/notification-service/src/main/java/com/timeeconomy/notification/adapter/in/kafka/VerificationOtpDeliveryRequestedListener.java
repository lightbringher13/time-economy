package com.timeeconomy.notification.adapter.in.kafka;

import com.timeeconomy.contracts.auth.v1.VerificationOtpDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleVerificationOtpDeliveryRequestedUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationOtpDeliveryRequestedListener {

    private final HandleVerificationOtpDeliveryRequestedUseCase useCase;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(
            topics = "${topics.auth.verification-otp-delivery-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(
            ConsumerRecord<String, VerificationOtpDeliveryRequestedV1> record,
            Acknowledgment ack
    ) {
        VerificationOtpDeliveryRequestedV1 event = record.value();

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

        log.info("Consumed topic={} key={} partition={} offset={} verificationChallengeId={}",
                record.topic(), record.key(), record.partition(), record.offset(),
                event.getVerificationChallengeId());
    }
}