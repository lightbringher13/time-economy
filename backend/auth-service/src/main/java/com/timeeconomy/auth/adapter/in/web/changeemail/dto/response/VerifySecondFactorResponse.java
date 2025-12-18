package com.timeeconomy.auth.adapter.in.web.changeemail.dto.response;

public record VerifySecondFactorResponse(
            Long requestId,
            String newEmail
    ) {}
