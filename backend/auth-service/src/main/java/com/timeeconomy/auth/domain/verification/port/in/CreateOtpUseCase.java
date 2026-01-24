package com.timeeconomy.auth.domain.verification.port.in;

import java.time.Instant;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface CreateOtpUseCase {

    CreateOtpResult createOtp(CreateOtpCommand command);

    record CreateOtpCommand(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,   // EMAIL / SMS
            String destination,            // raw email/phone from caller
            String requestIp,
            String userAgent
    ) {}

    enum Outcome {
        SENT,               // created + delivery requested
        THROTTLED,          // send limit / cooldown hit (do not create a new OTP)
        INVALID_INPUT,      // missing/invalid command fields
        DESTINATION_INVALID,// validation failed (email/phone format)
        INTERNAL_ERROR      // optional: if you prefer not to throw
    }

    record CreateOtpResult(
            Outcome outcome,
            String challengeId,        // nullable (THROTTLED/INVALID_INPUT)
            Instant expiresAt,         // nullable (THROTTLED/INVALID_INPUT)
            String maskedDestination,  // nullable
            Instant nextSendAt         // nullable; for UI "try again in N sec"
    ) {
        public boolean sent() { return outcome == Outcome.SENT; }
        public boolean throttled() { return outcome == Outcome.THROTTLED; }
    }
}