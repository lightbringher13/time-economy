package com.timeeconomy.auth_service.domain.port.in;

public interface RegisterUseCase {

    record RegisterCommand(
            String email,
            String password
    ) {}

    record RegisterResult(
            Long userId,
            String email
    ) {}

    RegisterResult register(RegisterCommand command);
}