// domain/port/in/ResetPasswordUseCase.java
package com.timeeconomy.auth_service.domain.port.in;

public interface ResetPasswordUseCase {

    record Command(
            String rawToken,
            String newPassword
    ) {}

    record Result(boolean success) {}

    Result resetPassword(Command command);
}