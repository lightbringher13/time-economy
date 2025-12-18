package com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.response;

import java.util.UUID;

public record SendSignupOtpResponse(
        boolean sent,
        UUID sessionId,
        String challengeId,
        int ttlMinutes,
        String maskedDestination,
        boolean emailVerified,
        boolean phoneVerified,
        String state
) {}