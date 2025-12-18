package com.timeeconomy.auth.adapter.in.web.verification.dto.response;

public record CreateOtpResponse(
        String challengeId,
        boolean sent,
        int ttlMinutes,
        String maskedDestination
) {}