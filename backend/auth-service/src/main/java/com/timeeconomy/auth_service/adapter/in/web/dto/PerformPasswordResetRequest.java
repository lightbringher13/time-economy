package com.timeeconomy.auth_service.adapter.in.web.dto;

public record PerformPasswordResetRequest(
        String newPassword,
        String confirmPassword
) {}
