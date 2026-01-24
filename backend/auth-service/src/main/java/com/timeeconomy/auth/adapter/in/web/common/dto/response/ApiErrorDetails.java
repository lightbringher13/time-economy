package com.timeeconomy.auth.adapter.in.web.common.dto.response;

import java.time.Instant;

public record ApiErrorDetails(
        Instant otpExpiresAt,
        Integer remainingAttempts,
        Integer retryAfterSeconds
) {}