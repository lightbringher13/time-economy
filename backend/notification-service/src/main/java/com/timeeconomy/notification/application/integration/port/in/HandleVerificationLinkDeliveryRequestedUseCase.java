package com.timeeconomy.notification.application.integration.port.in;

import com.timeeconomy.contracts.auth.v1.VerificationLinkDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;

public interface HandleVerificationLinkDeliveryRequestedUseCase {
    void handle(VerificationLinkDeliveryRequestedV1 event, ConsumerContext ctx);
}