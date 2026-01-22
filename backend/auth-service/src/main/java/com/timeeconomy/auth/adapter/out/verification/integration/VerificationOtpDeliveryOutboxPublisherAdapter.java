// auth/adapter/out/verification/outbox/VerificationOtpDeliveryOutboxPublisherAdapter.java
package com.timeeconomy.auth.adapter.out.verification.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import com.timeeconomy.auth.domain.verification.model.payload.VerificationOtpDeliveryRequestedPayload;
import com.timeeconomy.auth.domain.verification.port.out.integration.VerificationOtpDeliveryPublisherPort;

@Component
@RequiredArgsConstructor
public class VerificationOtpDeliveryOutboxPublisherAdapter implements VerificationOtpDeliveryPublisherPort {

    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final OutboxPayloadSerializerPort outboxPayloadSerializerPort;

    @Override
    public void publish(PublishCommand c) {
        // build payload object (still in verification domain)
        String payloadJson = outboxPayloadSerializerPort.serialize(
                new VerificationOtpDeliveryRequestedPayload(
                        c.challengeId(),
                        c.purpose(),
                        c.channel(),
                        c.subjectType(),
                        c.subjectId(),
                        c.normalizedDestination(),
                        c.ttlSeconds()
                )
        );

        OutboxEvent event = OutboxEvent.newPending(
                "verification_challenge",
                c.challengeId().toString(),
                "VerificationOtpDeliveryRequested.v1",
                payloadJson,
                c.occurredAt()
        );

        outboxEventRepositoryPort.save(event);
    }
}