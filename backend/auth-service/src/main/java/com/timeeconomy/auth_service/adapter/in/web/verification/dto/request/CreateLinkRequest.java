package com.timeeconomy.auth_service.adapter.in.web.verification.dto.request;

import com.timeeconomy.auth_service.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth_service.domain.verification.model.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLinkRequest(
        @NotNull VerificationPurpose purpose,
        @NotNull VerificationChannel channel,
        @NotBlank String destination,
        @NotBlank String linkBaseUrl
) {}