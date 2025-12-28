package com.timeeconomy.notification.adapter.out.jpa.inbox;

import com.timeeconomy.notification.adapter.out.jpa.inbox.mapper.ProcessedEventMapper;
import com.timeeconomy.notification.adapter.out.jpa.inbox.repository.ProcessedEventJpaRepository;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;
import com.timeeconomy.notification.domain.inbox.port.out.ProcessedEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcessedEventJpaAdapter implements ProcessedEventRepositoryPort {

    private final ProcessedEventJpaRepository repo;

    @Override
    @Transactional
    public boolean markProcessed(ProcessedEvent processedEvent) {
        try {
            repo.save(ProcessedEventMapper.toEntity(processedEvent));
            return true;
        } catch (DataIntegrityViolationException dup) {
            // uq_processed_events(consumer_group,event_id) 위반 → 이미 처리됨
            return false;
        }
    }
}