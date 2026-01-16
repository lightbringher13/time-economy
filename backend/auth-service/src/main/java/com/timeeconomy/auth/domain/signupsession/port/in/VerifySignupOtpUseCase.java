package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public interface VerifySignupOtpUseCase {

    Result verify(Command command);

    record Command(
            UUID sessionId, // may be null (cookie missing)
            SignupVerificationTarget target,
            String code
    ) {}

    enum Outcome {
        VERIFIED,        // correct code
        WRONG_CODE,      // incorrect code
        NO_SESSION,      // missing/expired session
        NO_PENDING_OTP,  // otp not requested / pending flag is false
        INVALID_INPUT    // missing/invalid target/code
    }

    record Result(
            Outcome outcome,
            boolean success,
            UUID sessionId,
            boolean emailVerified,
            boolean phoneVerified,
            boolean emailOtpPending,   // optional but useful
            boolean phoneOtpPending,   // optional but useful
            SignupSessionState state
    ) {}
}