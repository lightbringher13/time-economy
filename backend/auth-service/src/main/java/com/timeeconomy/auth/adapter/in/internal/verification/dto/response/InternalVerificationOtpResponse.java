package com.timeeconomy.auth.adapter.in.internal.verification.dto.response;

public record InternalVerificationOtpResponse(
        String verificationChallengeId,
        String otp
) {
    @Override
    public String toString() {
        return "InternalVerificationOtpResponse[verificationChallengeId="
                + verificationChallengeId
                + ", otp=[REDACTED]]";
    }
}
