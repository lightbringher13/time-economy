package com.timeeconomy.auth.adapter.in.web.signupsession.dto.response;

import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public record CancelSignupSessionResponse(
        UUID sessionId,
        SignupSessionState state
) {}