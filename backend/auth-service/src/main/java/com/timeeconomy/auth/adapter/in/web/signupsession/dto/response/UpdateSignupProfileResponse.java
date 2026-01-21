package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.UpdateSignupProfileUseCase;

public record UpdateSignupProfileResponse(
        UpdateSignupProfileUseCase.Outcome outcome,
        boolean updated,
        UUID sessionId,
        SignupSessionState state
) {}