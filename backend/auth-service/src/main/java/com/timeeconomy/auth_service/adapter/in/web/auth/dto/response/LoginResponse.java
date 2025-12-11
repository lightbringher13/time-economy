package com.timeeconomy.auth_service.adapter.in.web.auth.dto.response;

/**
 * JSON response body for successful login.
 * Refresh token will be set in httpOnly cookie.
 */
public record LoginResponse(
        String accessToken
) {}