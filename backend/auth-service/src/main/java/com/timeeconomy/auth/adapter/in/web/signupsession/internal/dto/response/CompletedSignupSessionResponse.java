// backend/auth-service/src/main/java/com/timeeconomy/auth/adapter/in/web/signupsession/internal/dto/CompletedSignupSessionResponse.java
package com.timeeconomy.auth.adapter.in.web.signupsession.internal.dto.response;

import java.time.LocalDate;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public record CompletedSignupSessionResponse(
        String email,
        String phoneNumber,
        String name,
        String gender,
        LocalDate birthDate,
        SignupSessionState state
) {}