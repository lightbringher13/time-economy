package com.timeeconomy.auth_service.adapter.in.web.passwordreset.dto.request;

public record PerformPasswordResetRequest(
        String newPassword,
        String confirmPassword
) {}
