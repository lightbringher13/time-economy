package com.timeeconomy.auth.domain.auth.port.in;

public interface LogoutSessionUseCase {

    void logoutSession(Command command);

    record Command(Long sessionId, String refreshToken) {}
}
