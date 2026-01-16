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
        SENT,               // OTP issued
        ALREADY_VERIFIED,   // target already verified; no OTP sent
        INVALID_DESTINATION,// missing/invalid email/phone
        THROTTLED           // resend blocked (cooldown/rate limit)
    }

    record Result(
            Outcome outcome,
            boolean sent,
            UUID sessionId,
            boolean sessionCreated,         // ✅ NEW: true if session was created in this call
            String challengeId,
            int ttlMinutes,
            String maskedDestination,
            boolean emailVerified,
            boolean phoneVerified,
            boolean emailOtpPending,        // ✅ NEW: fact
            boolean phoneOtpPending,        // ✅ NEW: fact
            SignupSessionState state        // derived state from facts
    ) {}
}