package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.time.LocalDate;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public record SignupStatusResponse(
        boolean exists,
        String email,
        boolean emailVerified,
        boolean emailOtpPending,    // ✅ NEW
        String phoneNumber,
        boolean phoneVerified,
        boolean phoneOtpPending,    // ✅ NEW
        String name,
        String gender,
        LocalDate birthDate,
        SignupSessionState state
) {}