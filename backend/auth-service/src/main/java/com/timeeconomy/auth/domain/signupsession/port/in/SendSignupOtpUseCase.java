// src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/SendSignupOtpUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public interface SendSignupOtpUseCase {

    Result send(Command command);

    record Command(
            UUID sessionId,                 // nullable (lazy-open)
            SignupVerificationTarget target,
            String destination,             // raw email/phone from client
            String requestIp,               // optional
            String userAgent                // optional
    ) {}

    enum Outcome {
        SENT,

        EMAIL_ALREADY_USED,
        PHONE_ALREADY_USED,

        ALREADY_VERIFIED,
        INVALID_DESTINATION,

        // ✅ ADD THESE (so we don't throw for expected cases)
        INVALID_STATE,      // e.g. phone OTP requested before email verified
        INVALID_INPUT,      // e.g. null target / null command

        THROTTLED
    }

    record Result(
            Outcome outcome,
            boolean sent,
            UUID sessionId,
            boolean sessionCreated,
            String challengeId,
            int ttlMinutes,
            String maskedDestination,
            boolean emailVerified,
            boolean phoneVerified,
            boolean emailOtpPending,
            boolean phoneOtpPending,
            SignupSessionState state
    ) {}
}