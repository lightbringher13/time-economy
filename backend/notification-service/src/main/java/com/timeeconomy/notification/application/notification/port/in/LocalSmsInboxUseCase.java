package com.timeeconomy.notification.application.notification.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalSmsInboxUseCase {

    record MessageSummary(
            UUID eventId,
            UUID challengeId,
            String maskedDestination,
            String templateKey,
            String purpose,
            Instant createdAt,
            Instant expiresAt
    ) {
    }

    record ConsumedMessage(
            UUID challengeId,
            String otp,
            Instant expiresAt
    ) {
        @Override
        public String toString() {
            return "ConsumedMessage[challengeId=" + challengeId
                    + ", otp=<redacted>, expiresAt=" + expiresAt + "]";
        }
    }

    List<MessageSummary> findRecent(int limit);

    Optional<ConsumedMessage> consume(UUID challengeId);
}
