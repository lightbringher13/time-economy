package com.timeeconomy.auth.domain.auth.model.payload;

import java.util.UUID;

public record AuthUserRegisteredPayload(
        Long userId,
        UUID signupSessionId
) {}