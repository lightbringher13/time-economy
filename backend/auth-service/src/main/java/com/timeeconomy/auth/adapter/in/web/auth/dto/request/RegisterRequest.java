package com.timeeconomy.auth.adapter.in.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
            @NotBlank String password
    ) {}