package com.timeeconomy.auth.domain.outbox.port.out;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;

public interface OutboxEventPublisherPort {
    void publish(OutboxEvent event);
}