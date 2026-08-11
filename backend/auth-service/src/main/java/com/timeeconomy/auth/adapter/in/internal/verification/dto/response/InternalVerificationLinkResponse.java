package com.timeeconomy.auth.adapter.in.internal.verification.dto.response;

public record InternalVerificationLinkResponse(
        String verificationChallengeId,
        String linkUrl
) {
    @Override
    public String toString() {
        return "InternalVerificationLinkResponse[verificationChallengeId="
                + verificationChallengeId
                + ", linkUrl=[REDACTED]]";
    }
}
