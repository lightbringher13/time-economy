package com.timeeconomy.auth_service.adapter.in.web.auth.dto.request;

/**
 * JSON body for POST /api/auth/login
 */
public record LoginRequest(
        String email,
        String password,
        String deviceInfo,
        String ipAddress,
        String userAgent
) {}