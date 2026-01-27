package com.timeeconomy.auth.domain.signupsession.port.in;

import java.time.Instant;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public interface VerifySignupOtpUseCase {

    Result verify(Command command);

    record Command(
            UUID sessionId,                 // may be null (cookie missing)
            SignupVerificationTarget target,
            String code,
            String requestIp                // ✅ NEW (nullable)
    ) {}

    enum Outcome {
        VERIFIED,
        WRONG_CODE,
        EXPIRED,         // ✅ from Verification
        LOCKED,          // ✅ from Verification
        THROTTLED,       // ✅ from Verification
        NO_SESSION,
        NO_PENDING_OTP,
        INVALID_INPUT
    }

    record Result(
            Outcome outcome,
            boolean success,
            UUID sessionId,

            // ✅ passthrough from Verification (nice UX)
            Instant otpExpiresAt,           // nullable
            Integer remainingAttempts,      // nullable
            Integer retryAfterSeconds,      // nullable (only THROTTLED)

            // session facts
            boolean emailVerified,
            boolean phoneVerified,
            boolean emailOtpPending,
            boolean phoneOtpPending,
            SignupSessionState state
    ) {}
}