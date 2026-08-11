package com.timeeconomy.notification.application.integration.exception;

import java.util.UUID;

public final class VerificationLinkDeliveryRetryException extends RuntimeException {

    public VerificationLinkDeliveryRetryException(
            UUID eventId,
            UUID challengeId,
            Class<? extends Throwable> failureType
    ) {
        super("Verification link delivery failed eventId=" + eventId
                + " challengeId=" + challengeId
                + " failureType=" + failureType.getSimpleName());
    }
}
