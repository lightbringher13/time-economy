package com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.response;

public record VerifySecondFactorResponse(
            Long requestId,
            String newEmail
    ) {}
