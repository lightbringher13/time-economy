package com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.request;

 public record VerifyNewEmailCodeRequest(
            Long requestId,
            String code
    ) {}
