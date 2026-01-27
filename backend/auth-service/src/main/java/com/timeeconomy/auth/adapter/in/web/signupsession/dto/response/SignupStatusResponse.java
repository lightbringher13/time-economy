// backend/auth-service/src/main/java/com/timeeconomy/auth/adapter/in/web/signupsession/dto/response/SignupStatusResponse.java
package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public record SignupStatusResponse(
        boolean exists,

        String email,
        boolean emailVerified,
        boolean emailOtpPending,
        Instant emailOtpExpiresAt,     // ✅ NEW

        String phoneNumber,
        boolean phoneVerified,
        boolean phoneOtpPending,
        Instant phoneOtpExpiresAt,     // ✅ NEW

        String name,
        String gender,
        LocalDate birthDate,

        SignupSessionState state
) {}