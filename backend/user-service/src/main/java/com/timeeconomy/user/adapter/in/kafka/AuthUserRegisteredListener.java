package com.timeeconomy.user.adapter.in.kafka;

import com.timeeconomy.user.application.userprofile.port.in.HandleAuthUserRegisteredUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserRegisteredListener {

    private final HandleAuthUserRegisteredUseCase useCase;

    @KafkaListener(
        topics = "${topics.auth.user-registered}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, AuthUserRegisteredV1> record, Acknowledgment ack) {

        AuthUserRegisteredV1 event = record.value();

        useCase.handle(event);

        ack.acknowledge();

        log.info("Consumed topic={} key={} partition={} offset={} userId={}",
                record.topic(), record.key(), record.partition(), record.offset(),
                event.getUserId());
    }
}