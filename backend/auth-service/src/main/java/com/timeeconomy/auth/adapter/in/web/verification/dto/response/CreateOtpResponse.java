package com.timeeconomy.auth.adapter.in.web.verification.dto.response;

import java.time.Instant;

public record CreateOtpResponse(
        String challengeId,
        boolean sent,
        Instant expiresAt,
        String maskedDestination
) {}