package com.timeeconomy.notification.adapter.out.authinternal.dto;

public record OtpOnceResponse(
        String verificationChallengeId,
        String otp
) {}