package com.timeeconomy.auth.domain.auth.port.in;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
    
    public record LogoutCommand(String refreshToken) {}
}
