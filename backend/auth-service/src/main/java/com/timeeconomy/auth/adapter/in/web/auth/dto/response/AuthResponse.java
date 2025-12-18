package com.timeeconomy.auth.adapter.in.web.auth.dto.response;

public record AuthResponse(
        String accessToken
        // later you can add: String tokenType, Long userId, String email, ...
) {}