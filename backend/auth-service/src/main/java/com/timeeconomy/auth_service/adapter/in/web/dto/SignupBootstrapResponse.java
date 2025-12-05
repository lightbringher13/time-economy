package com.timeeconomy.auth_service.adapter.in.web.dto;

import java.time.LocalDate;

public record SignupBootstrapResponse(
        boolean hasSession,
        String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String name,
        String gender,
        LocalDate birthDate,
        String state   // e.g. "EMAIL_PENDING", "EMAIL_VERIFIED", ...
) {}