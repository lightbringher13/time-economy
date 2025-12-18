// src/main/java/com/timeeconomy/auth_service/adapter/in/web/signup/dto/SendSignupOtpRequest.java
package com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.request;

import com.timeeconomy.auth_service.domain.signupsession.model.SignupVerificationTarget;
import jakarta.validation.constraints.NotNull;

public record SendSignupOtpRequest(
        @NotNull SignupVerificationTarget target
) {}