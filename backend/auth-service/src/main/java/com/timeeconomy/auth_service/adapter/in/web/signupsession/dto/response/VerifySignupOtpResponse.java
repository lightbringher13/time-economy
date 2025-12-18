package com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.response;

import java.util.UUID;

public record VerifySignupOtpResponse(
        boolean success,
        UUID sessionId,
        boolean emailVerified,
        boolean phoneVerified,
        String state
) {}