package com.timeeconomy.notification.application.integration.port.in;

import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;

public interface HandleAuthUserRegisteredUseCase {
    void handle(AuthUserRegisteredV1 event, ConsumerContext ctx);
}