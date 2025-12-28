package com.timeeconomy.notification.application.integration.port.in;

import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;

public interface HandleEmailChangeCommittedUseCase {
    void handle(EmailChangeCommittedV1 event, ConsumerContext ctx);
}