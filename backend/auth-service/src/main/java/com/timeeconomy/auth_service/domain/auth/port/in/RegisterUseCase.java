package com.timeeconomy.auth_service.domain.auth.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface RegisterUseCase {

    record RegisterCommand(
            UUID signupSessionId,   // ⭐ NEW
            String email,
            String password,
            String phoneNumber,
            String name,
            String gender,
            LocalDate birthDate
    ) {}

    record RegisterResult(
            Long userId,
            String email
    ) {}

    RegisterResult register(RegisterCommand command);
}