package com.timeeconomy.auth_service.adapter.out.http.dto;

public record CreateUserProfileRequest(
        Long userId,
        String email
) {}