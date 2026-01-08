package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public record EditSignupPhoneResponse(
        UUID sessionId,
        String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String name,
        String gender,
        LocalDate birthDate,
        SignupSessionState state
) {}