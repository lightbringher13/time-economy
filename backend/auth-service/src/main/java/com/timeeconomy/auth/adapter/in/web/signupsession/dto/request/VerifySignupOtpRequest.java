package com.timeeconomy.auth.adapter.in.web.signupsession.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public record VerifySignupOtpRequest(
        UUID sessionId, // optional (cookie fallback)

        @NotNull
        SignupVerificationTarget target,

        @NotBlank
        @Size(min = 4, max = 10) // 6 is typical, but allow flexibility
        String code
) {}