// src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/SendSignupOtpUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.time.Instant;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public interface SendSignupOtpUseCase {

    Result send(Command command);

    record Command(
            UUID sessionId,
            SignupVerificationTarget target,
            String destination,
            String requestIp,
            String userAgent
    ) {}

    enum Outcome {
        SENT,

        EMAIL_ALREADY_USED,
        PHONE_ALREADY_USED,

        ALREADY_VERIFIED,
        INVALID_DESTINATION,

        INVALID_STATE,
        INVALID_INPUT,

        THROTTLED,
        DELIVERY_FAILED   // ✅ strongly recommended
    }

    record Result(
            Outcome outcome,
            boolean sent,
            UUID sessionId,
            boolean sessionCreated,
            String challengeId,
            int ttlMinutes,
            String maskedDestination,

            Integer retryAfterSeconds,      // ✅ NEW (nullable)

            Instant emailOtpExpiresAt,
            Instant phoneOtpExpiresAt,
            boolean emailVerified,
            boolean phoneVerified,
            boolean emailOtpPending,
            boolean phoneOtpPending,
            SignupSessionState state
    ) {}
}