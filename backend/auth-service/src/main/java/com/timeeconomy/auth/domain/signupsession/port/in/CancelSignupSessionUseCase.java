// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/CancelSignupSessionUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface CancelSignupSessionUseCase {

    record Command(UUID sessionId) {}

    record Result(
            UUID sessionId,
            SignupSessionState state
    ) {}

    Result cancel(Command command);
}