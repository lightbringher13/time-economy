// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/changeemail/port/in/CancelEmailChangeUseCase.java
package com.timeeconomy.auth.domain.changeemail.port.in;

public interface CancelEmailChangeUseCase {

    void cancel(CancelCommand command);

    record CancelCommand(Long userId, Long requestId) {}
}