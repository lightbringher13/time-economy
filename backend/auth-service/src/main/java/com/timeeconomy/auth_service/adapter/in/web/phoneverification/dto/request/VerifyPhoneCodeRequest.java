package com.timeeconomy.auth_service.adapter.in.web.phoneverification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyPhoneCodeRequest(
        @NotBlank String phoneNumber,
        @NotBlank String code
) {}