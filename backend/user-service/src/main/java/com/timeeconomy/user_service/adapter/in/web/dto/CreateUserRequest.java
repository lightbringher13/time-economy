package com.timeeconomy.user_service.adapter.in.web.dto;

public record CreateUserRequest(
        Long userId,
        String email
) {}