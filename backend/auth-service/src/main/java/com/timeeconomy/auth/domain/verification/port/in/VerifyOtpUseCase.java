package com.timeeconomy.auth.domain.verification.port.in;

import java.time.Instant;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface VerifyOtpUseCase {

    VerifyOtpResult verifyOtp(VerifyOtpCommand command);

    record VerifyOtpCommand(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String destination,
            String code,
            String requestIp          // ✅ NEW (optional but recommended for limiter)
    ) {}

    enum Outcome {
        VERIFIED,
        WRONG_CODE,
        EXPIRED,
        LOCKED,                 // max attempts reached (domain)
        NO_PENDING_CHALLENGE,
        DESTINATION_MISMATCH,
        THROTTLED,              // ✅ NEW (rate-limit / cooldown)
        INVALID_INPUT
    }

    record VerifyOtpResult(
            Outcome outcome,
            Instant expiresAt,            // nullable (if no challenge)
            Integer remainingAttempts,    // nullable (if no challenge)
            Integer retryAfterSeconds     // ✅ NEW (nullable; only when THROTTLED)
    ) {
        public boolean success() { return outcome == Outcome.VERIFIED; }
        public boolean throttled() { return outcome == Outcome.THROTTLED; }
    }
}