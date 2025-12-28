package com.timeeconomy.auth.adapter.out.kafka.outbox.publisher;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventPublisherPort;
import lombok.RequiredArgsConstructor;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.kafka.outbox.mapper.OutboxAvroMapper;
import com.timeeconomy.auth.adapter.out.kafka.outbox.topic.OutboxTopicResolver;
import com.timeeconomy.auth.domain.outbox.exception.OutboxPublishFailedException;
import java.util.concurrent.ExecutionException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OutboxKafkaAvroPublisherAdapter implements OutboxEventPublisherPort {

    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final OutboxTopicResolver topicResolver; // eventType -> topic
    private final OutboxAvroMapper avroMapper;

    @Override
    public void publish(OutboxEvent event) {
        String topic = topicResolver.resolveTopic(event.getEventType());
        String key = event.getAggregateId();

        SpecificRecord avroRecord = avroMapper.toAvro(event);

        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>(topic, key, avroRecord);
        record.headers().add("event_id", event.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event_type", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).get(); // wait for broker ack
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // ✅ important
            throw new OutboxPublishFailedException("Interrupted while publishing outbox event " + event.getId(), e);
        } catch (ExecutionException e) {
            // real cause is often inside e.getCause()
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            throw new OutboxPublishFailedException("Failed to publish outbox event " + event.getId(), cause);
        }
    }
}