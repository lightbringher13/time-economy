package com.timeeconomy.auth.adapter.in.web.auth.dto.response;

public record RegisterResponse(
        Long userId,
        String email
) {}