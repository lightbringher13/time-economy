package com.timeeconomy.auth_service.adapter.in.web.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
        // 👉 FE에서만 confirmPassword 체크하면, BE에는 굳이 안 보내도 됨
        // String confirmPassword
) {}