package com.timeeconomy.notification.adapter.out.authclient.dto.response;

public record LinkUrlPeekResponse(
        String verificationChallengeId,
        String linkUrl
) {
    @Override
    public String toString() {
        return "LinkUrlPeekResponse[verificationChallengeId=" + verificationChallengeId
                + ", linkUrl=<redacted>]";
    }
}
