// backend/auth-service/src/main/java/com/timeeconomy/auth/adapter/out/verification/outbox/VerificationLinkDeliveryOutboxPublisherAdapter.java
package com.timeeconomy.auth.adapter.out.verification.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import com.timeeconomy.auth.domain.verification.model.payload.VerificationLinkDeliveryRequestedPayload;
import com.timeeconomy.auth.domain.verification.port.out.integration.VerificationLinkDeliveryPublisherPort;

@Component
@RequiredArgsConstructor
public class VerificationLinkDeliveryOutboxPublisherAdapter implements VerificationLinkDeliveryPublisherPort {

    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final OutboxPayloadSerializerPort outboxPayloadSerializerPort;

    @Override
    public void publish(PublishCommand command) {
        String payloadJson = outboxPayloadSerializerPort.serialize(
                new VerificationLinkDeliveryRequestedPayload(
                        command.challengeId(),
                        command.purpose(),
                        command.channel(),
                        command.subjectType(),
                        command.subjectId(),
                        command.normalizedDestination(),
                        command.ttlSeconds()
                )
        );

        OutboxEvent event = OutboxEvent.newPending(
                "verification_challenge",
                command.challengeId().toString(),
                "VerificationLinkDeliveryRequested.v1",
                payloadJson,
                command.occurredAt()
        );

        outboxEventRepositoryPort.save(event);
    }
}