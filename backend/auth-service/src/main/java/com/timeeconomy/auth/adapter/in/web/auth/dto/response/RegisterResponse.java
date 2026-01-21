package com.timeeconomy.auth.adapter.in.web.auth.dto.response;

import com.timeeconomy.auth.domain.auth.port.in.RegisterUseCase;

public record RegisterResponse(
        RegisterUseCase.Outcome outcome,
        boolean registered,
        Long userId,
        String email
) {}