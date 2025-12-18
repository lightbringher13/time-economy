package com.timeeconomy.auth.adapter.in.web.changeemail.dto.request;

 public record VerifyNewEmailCodeRequest(
            Long requestId,
            String code
    ) {}
