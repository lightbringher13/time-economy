package com.timeeconomy.notification.application.notification.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface LocalSmsInboxPort {

    record Entry(
            UUID eventId,
            UUID challengeId,
            String otp,
            String maskedDestination,
            String templateKey,
            String purpose,
            Instant createdAt,
            Instant expiresAt
    ) {
        public Entry {
            Objects.requireNonNull(eventId, "eventId");
            Objects.requireNonNull(challengeId, "challengeId");
            Objects.requireNonNull(otp, "otp");
            Objects.requireNonNull(maskedDestination, "maskedDestination");
            Objects.requireNonNull(templateKey, "templateKey");
            Objects.requireNonNull(purpose, "purpose");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }

        @Override
        public String toString() {
            return "Entry[eventId=" + eventId
                    + ", challengeId=" + challengeId
                    + ", otp=<redacted>, maskedDestination=" + maskedDestination
                    + ", templateKey=" + templateKey
                    + ", purpose=" + purpose
                    + ", createdAt=" + createdAt
                    + ", expiresAt=" + expiresAt + "]";
        }
    }

    void store(Entry entry);

    List<Entry> findRecent(int limit);

    Optional<Entry> consume(UUID challengeId);
}
