package com.timeeconomy.notification.adapter.out.authclient.dto.response;

public record OtpOnceResponse(
        String verificationChallengeId,
        String otp
) {}