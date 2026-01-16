package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface CancelSignupSessionUseCase {

    Result cancel(Command command);

    record Command(UUID sessionId) {} // may be null

    enum Outcome {
        CANCELED,        // canceled now
        ALREADY_TERMINAL,// already CANCELED/EXPIRED/COMPLETED
        NO_SESSION,      // missing/expired/not found
        INVALID_INPUT
    }

    record Result(
            Outcome outcome,
            boolean canceled,
            UUID sessionId,
            SignupSessionState state
    ) {}
}