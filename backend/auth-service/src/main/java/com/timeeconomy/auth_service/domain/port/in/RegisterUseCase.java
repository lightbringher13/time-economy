package com.timeeconomy.auth_service.domain.port.in;

import java.time.LocalDate;

public interface RegisterUseCase {

    record RegisterCommand(
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