package com.timeeconomy.auth.domain.verification.port.in.internal;

public interface PeekVerificationOtpUseCase {

    record Command(String challengeId) {}

    record Result(String verificationChallengeId, String otp) {
        @Override
        public String toString() {
            return "Result[verificationChallengeId=" + verificationChallengeId + ", otp=[REDACTED]]";
        }
    }

    Result peek(Command command);
}
