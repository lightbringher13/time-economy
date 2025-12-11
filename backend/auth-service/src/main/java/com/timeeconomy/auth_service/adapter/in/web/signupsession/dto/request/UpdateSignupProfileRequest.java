package com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.request;

import java.time.LocalDate;

public record UpdateSignupProfileRequest(
        String email,          // ✅ NEW: 이메일
        String name,
        String phoneNumber,
        String gender,         // "MALE" | "FEMALE" | "OTHER"
        LocalDate birthDate
) {}