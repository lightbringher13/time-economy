package com.timeeconomy.auth.domain.verification.model.payload;

import java.util.UUID;

public record VerificationOtpDeliveryRequestedPayload(
        UUID verificationChallengeId,
        String purpose,
        String channel,
        String subjectType,
        String subjectId,
        String destinationNorm,
        int ttlSeconds
) {}