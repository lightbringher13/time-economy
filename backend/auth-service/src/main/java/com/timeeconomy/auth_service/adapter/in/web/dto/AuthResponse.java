package com.timeeconomy.auth_service.adapter.in.web.dto;

public record AuthResponse(
        String accessToken
        // later you can add: String tokenType, Long userId, String email, ...
) {}