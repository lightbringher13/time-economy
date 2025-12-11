package com.timeeconomy.auth_service.adapter.in.web.auth.dto.response;

public record RegisterResponse(
        Long userId,
        String email
) {}