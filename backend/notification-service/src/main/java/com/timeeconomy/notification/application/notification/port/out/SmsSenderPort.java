package com.timeeconomy.notification.application.notification.port.out;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public interface SmsSenderPort {

    record SmsSendCommand(
            UUID eventId,
            UUID challengeId,
            String templateKey,
            String toPhone,
            String otp,
            String purpose,
            Instant expiresAt
    ) {
        public SmsSendCommand {
            Objects.requireNonNull(eventId, "eventId");
            Objects.requireNonNull(challengeId, "challengeId");
            Objects.requireNonNull(templateKey, "templateKey");
            Objects.requireNonNull(toPhone, "toPhone");
            Objects.requireNonNull(otp, "otp");
            Objects.requireNonNull(purpose, "purpose");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }

        @Override
        public String toString() {
            return "SmsSendCommand[eventId=" + eventId
                    + ", challengeId=" + challengeId
                    + ", templateKey=" + templateKey
                    + ", toPhone=<redacted>, otp=<redacted>, purpose=" + purpose
                    + ", expiresAt=" + expiresAt + "]";
        }
    }

    record SmsSendResult(
            String provider,
            String providerMsgId
    ) {}

    SmsSendResult sendTemplate(SmsSendCommand command);
}
