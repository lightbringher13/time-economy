package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.time.LocalDate;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public record SignupStatusResponse(
        boolean exists,
        String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String name,
        String gender,
        LocalDate birthDate,
        SignupSessionState state
) {}