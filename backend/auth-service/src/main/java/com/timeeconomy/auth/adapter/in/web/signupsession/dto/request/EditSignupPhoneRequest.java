package com.timeeconomy.auth.adapter.in.web.signupsession.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditSignupPhoneRequest(
        @NotBlank String newPhoneNumber
) {}