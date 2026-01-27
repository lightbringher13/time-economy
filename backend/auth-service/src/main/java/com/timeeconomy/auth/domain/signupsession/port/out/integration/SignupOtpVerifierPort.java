// auth/domain/signupsession/port/out/integration/SignupOtpVerifierPort.java
package com.timeeconomy.auth.domain.signupsession.port.out.integration;

import java.time.Instant;

public interface SignupOtpVerifierPort {

    enum Purpose { SIGNUP_EMAIL, SIGNUP_PHONE }
    enum Channel { EMAIL, SMS }

    record VerifyCommand(
            String subjectId,      // signupSessionId
            Purpose purpose,
            Channel channel,
            String destination,    // email/phone stored in session
            String code,
            String requestIp       // ✅ NEW (nullable)
    ) {}

    enum Outcome {
        VERIFIED,
        WRONG_CODE,
        EXPIRED,
        LOCKED,
        NO_PENDING_CHALLENGE,
        DESTINATION_MISMATCH,
        THROTTLED,              // ✅ NEW
        INVALID_INPUT,
        DELIVERY_FAILED
    }

    record VerifyResult(
            Outcome outcome,
            Instant expiresAt,            // nullable
            Integer remainingAttempts,    // nullable
            Integer retryAfterSeconds     // ✅ NEW (nullable; when THROTTLED)
    ) {
        public boolean success() { return outcome == Outcome.VERIFIED; }
        public boolean throttled() { return outcome == Outcome.THROTTLED; }
    }

    VerifyResult verify(VerifyCommand command);
}