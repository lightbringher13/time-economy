package com.timeeconomy.auth_service.domain.port.in;

public interface LogoutAllUseCase {
    void logoutAll(LogoutAllCommand command);

    record LogoutAllCommand(String refreshToken) {}
}
