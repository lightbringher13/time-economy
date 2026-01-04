package com.timeeconomy.notification.adapter.out.authclient.dto.response;

public record LinkUrlOnceResponse(
        String verificationChallengeId,
        String linkUrl
) {}