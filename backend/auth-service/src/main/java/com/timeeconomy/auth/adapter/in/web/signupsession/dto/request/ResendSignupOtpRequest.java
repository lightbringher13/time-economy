package com.timeeconomy.auth.adapter.in.web.signupsession.dto.request;

import jakarta.validation.constraints.NotNull;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public record ResendSignupOtpRequest(
        @NotNull SignupVerificationTarget target
) {}