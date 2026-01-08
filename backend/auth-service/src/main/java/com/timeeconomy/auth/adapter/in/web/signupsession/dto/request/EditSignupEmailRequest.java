package com.timeeconomy.auth.adapter.in.web.signupsession.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EditSignupEmailRequest(
        @NotBlank @Email String newEmail
) {}