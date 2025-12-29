package com.timeeconomy.notification.application.integration.port.in;

import com.timeeconomy.contracts.auth.v1.VerificationOtpDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;

public interface HandleVerificationOtpDeliveryRequestedUseCase {
    void handle(VerificationOtpDeliveryRequestedV1 event, ConsumerContext ctx);
}