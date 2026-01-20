package com.timeeconomy.auth.domain.auth.port.in;

import java.util.UUID;

public interface RegisterUseCase {

    record RegisterCommand(
            UUID signupSessionId,
            String password
    ) {}

    enum Outcome {
        REGISTERED,

        // session / state
        NO_SESSION,
        SESSION_EXPIRED_OR_NOT_FOUND,
        INVALID_STATE,          // not PROFILE_READY etc.

        // duplicates
        EMAIL_ALREADY_USED,
        PHONE_ALREADY_USED,

        // validation
        INVALID_INPUT
    }

    record RegisterResult(
            Outcome outcome,
            boolean registered,
            UUID signupSessionId,
            Long userId,
            String email
    ) {}

    RegisterResult register(RegisterCommand command);
}