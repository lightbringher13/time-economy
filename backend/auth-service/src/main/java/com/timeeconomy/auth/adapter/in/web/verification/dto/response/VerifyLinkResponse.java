package com.timeeconomy.auth.adapter.in.web.verification.dto.response;

public record VerifyLinkResponse(
        boolean success,
        String challengeId,
        String destinationNorm
) {}