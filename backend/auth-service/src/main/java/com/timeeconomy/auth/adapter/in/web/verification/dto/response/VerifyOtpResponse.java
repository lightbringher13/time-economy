package com.timeeconomy.auth.adapter.in.web.verification.dto.response;

import java.time.Instant;

public record VerifyOtpResponse(
        boolean success,
        String outcome,
        Instant expiresAt,
        Integer remainingAttempts,
        Integer retryAfterSeconds
) {}