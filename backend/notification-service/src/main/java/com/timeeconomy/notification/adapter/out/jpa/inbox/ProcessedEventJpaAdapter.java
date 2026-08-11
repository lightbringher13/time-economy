package com.timeeconomy.notification.adapter.out.jpa.inbox;

import com.timeeconomy.notification.adapter.out.jpa.inbox.repository.ProcessedEventJpaRepository;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;
import com.timeeconomy.notification.domain.inbox.port.out.ProcessedEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcessedEventJpaAdapter implements ProcessedEventRepositoryPort {

    private final ProcessedEventJpaRepository repo;

    @Override
    @Transactional
    public boolean markProcessed(ProcessedEvent processedEvent) {
        int insertedRows = repo.insertIfAbsent(
                processedEvent.getConsumerGroup(),
                processedEvent.getEventId(),
                processedEvent.getEventType(),
                processedEvent.getTopic(),
                processedEvent.getKafkaPartition(),
                processedEvent.getKafkaOffset(),
                processedEvent.getProcessedAt()
        );

        if (insertedRows == 1) {
            return true;
        }

        if (insertedRows == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected processed-event insert count: " + insertedRows);
    }
}
