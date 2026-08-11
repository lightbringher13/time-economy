package com.timeeconomy.notification.adapter.out.authclient.dto.response;

public record OtpPeekResponse(
        String verificationChallengeId,
        String otp
) {
    @Override
    public String toString() {
        return "OtpPeekResponse[verificationChallengeId=" + verificationChallengeId
                + ", otp=<redacted>]";
    }
}
