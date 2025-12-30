package com.timeeconomy.notification.application.integration.port.in;

import com.timeeconomy.contracts.auth.v2.AuthUserRegisteredV2;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;

public interface HandleAuthUserRegisteredUseCase {
    void handle(AuthUserRegisteredV2 event, ConsumerContext ctx);
}