package com.timeeconomy.auth_service.adapter.in.web.verification.dto.response;

public record CreateLinkResponse(
        String challengeId,
        boolean sent,
        int ttlMinutes,
        String maskedDestination
) {}