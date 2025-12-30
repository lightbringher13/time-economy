package com.timeeconomy.auth.domain.auth.model.payload;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AuthUserRegisteredPayload(
        Long userId,
        String email,
        String phoneNumber,
        String name,
        String gender,
        LocalDate birthDate,
        UUID signupSessionId,
        Instant occurredAt
) {}