// backend/auth-service/src/main/java/com/timeeconomy/auth/adapter/in/web/signupsession/dto/response/CancelSignupSessionResponse.java
package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.CancelSignupSessionUseCase;

public record CancelSignupSessionResponse(
        CancelSignupSessionUseCase.Outcome outcome,
        boolean canceled,
        UUID sessionId,
        SignupSessionState state
) {}