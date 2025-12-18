package com.timeeconomy.auth.adapter.in.web.passwordreset.dto.request;

public record PerformPasswordResetRequest(
        String newPassword,
        String confirmPassword
) {}
