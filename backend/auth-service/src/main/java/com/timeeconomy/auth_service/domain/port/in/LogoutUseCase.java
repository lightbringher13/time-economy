package com.timeeconomy.auth_service.domain.port.in;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
    
    public record LogoutCommand(String refreshToken) {}
}
