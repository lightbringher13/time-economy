package com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.request;

 public record VerifySecondFactorRequest(
            Long requestId,
            String code
    ) {}
