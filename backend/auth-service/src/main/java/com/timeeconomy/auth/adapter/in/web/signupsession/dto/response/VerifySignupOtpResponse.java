package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.VerifySignupOtpUseCase;

public record VerifySignupOtpResponse(
        VerifySignupOtpUseCase.Outcome outcome,
        boolean success,
        UUID sessionId,

        // ✅ NEW: useful for UI
        Instant otpExpiresAt,
        Integer remainingAttempts,
        Integer retryAfterSeconds,

        boolean emailVerified,
        boolean phoneVerified,
        boolean emailOtpPending,
        boolean phoneOtpPending,
        SignupSessionState state
) {}