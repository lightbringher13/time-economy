package com.timeeconomy.notification.domain.inbox.port.out;

import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;

public interface ProcessedEventRepositoryPort {

    /**
     * @return true = 처음 처리(삽입 성공), false = 이미 처리됨(중복)
     */
    boolean markProcessed(ProcessedEvent processedEvent);
}