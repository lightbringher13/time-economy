package com.timeeconomy.auth_service.adapter.in.web.dto;

public record RegisterRequest(
        String email,
        String password
) {}