// auth/domain/verification/port/out/VerificationOtpDeliveryPublisherPort.java
package com.timeeconomy.auth.domain.verification.port.out.integration;

import java.time.Instant;
import java.util.UUID;

public interface VerificationOtpDeliveryPublisherPort {

    record PublishCommand(
            UUID challengeId,
            String purpose,
            String channel,
            String subjectType,
            String subjectId,
            String normalizedDestination,
            int ttlSeconds,
            Instant occurredAt
    ) {}

    void publish(PublishCommand command);
}