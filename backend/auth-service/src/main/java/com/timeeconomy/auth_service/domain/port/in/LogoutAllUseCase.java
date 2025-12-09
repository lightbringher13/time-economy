// auth-service/src/main/java/com/timeeconomy/auth_service/domain/port/in/LogoutAllUseCase.java
package com.timeeconomy.auth_service.domain.port.in;

public interface LogoutAllUseCase {

    record LogoutAllCommand(Long authUserId) {}

    void logoutAll(LogoutAllCommand command);
}