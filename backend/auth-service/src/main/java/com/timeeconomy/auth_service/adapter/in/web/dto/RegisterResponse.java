package com.timeeconomy.auth_service.adapter.in.web.dto;

public record RegisterResponse(
        Long userId,
        String email
) {}