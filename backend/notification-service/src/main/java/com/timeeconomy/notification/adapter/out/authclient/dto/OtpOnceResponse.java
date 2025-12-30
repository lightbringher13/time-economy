package com.timeeconomy.notification.adapter.out.authclient.dto;

public record OtpOnceResponse(
        String verificationChallengeId,
        String otp
) {}