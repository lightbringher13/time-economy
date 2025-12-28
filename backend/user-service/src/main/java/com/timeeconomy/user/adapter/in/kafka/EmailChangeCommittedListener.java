package com.timeeconomy.user.adapter.in.kafka;

import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;
import com.timeeconomy.user.application.userprofile.port.in.HandleEmailChangeCommittedUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChangeCommittedListener {

    private final HandleEmailChangeCommittedUseCase useCase;

    @KafkaListener(
        topics = "${topics.auth.email-changed}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, EmailChangeCommittedV1> record, Acknowledgment ack) {

        EmailChangeCommittedV1 event = record.value();

        useCase.handle(event);

        ack.acknowledge();

        log.info("Consumed topic={} key={} partition={} offset={} userId={}",
                record.topic(), record.key(), record.partition(), record.offset(),
                event.getUserId());
    }
}