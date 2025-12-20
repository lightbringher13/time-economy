package com.timeeconomy.user.adapter.in.kafka;

import com.timeeconomy.user.adapter.in.kafka.event.EmailChangeCommittedV1;
import com.timeeconomy.user.application.userprofile.port.in.HandleEmailChangeCommittedUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChangeCommittedListener {

    private final JsonMapper jsonMapper;
    private final HandleEmailChangeCommittedUseCase useCase;

    @KafkaListener(
        topics = "${topics.auth.email-changed}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        EmailChangeCommittedV1 event =
                jsonMapper.readValue(record.value(), EmailChangeCommittedV1.class);

        useCase.handle(event);
        ack.acknowledge();

        log.info("Consumed topic={} key={} offset={} userId={}",
                record.topic(), record.key(), record.offset(), event.userId());
    }
}