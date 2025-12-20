package com.timeeconomy.user.adapter.in.kafka.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuthUserRegisteredV1(
        Long userId,
        String email,
        String phoneNumber,
        String name,
        String gender,
        LocalDate birthDate,
        UUID signupSessionId,
        LocalDateTime occurredAt
) {}