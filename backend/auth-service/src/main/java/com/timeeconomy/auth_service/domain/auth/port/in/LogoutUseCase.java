package com.timeeconomy.auth_service.domain.auth.port.in;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
    
    public record LogoutCommand(String refreshToken) {}
}
