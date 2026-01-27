package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.util.UUID;
import java.time.Instant;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.SendSignupOtpUseCase;

public record SendSignupOtpResponse(
        SendSignupOtpUseCase.Outcome outcome,
        boolean sent,
        UUID sessionId,
        boolean sessionCreated,
        String challengeId,
        int ttlMinutes,
        String maskedDestination,

        Integer retryAfterSeconds,        // ✅ NEW

        Instant emailOtpExpiresAt,        // ✅ NEW
        Instant phoneOtpExpiresAt,        // ✅ NEW

        boolean emailVerified,
        boolean phoneVerified,
        boolean emailOtpPending,
        boolean phoneOtpPending,
        SignupSessionState state
) {}