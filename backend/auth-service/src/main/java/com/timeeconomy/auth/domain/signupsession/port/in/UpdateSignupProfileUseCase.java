package com.timeeconomy.auth.domain.signupsession.port.in;

import java.time.LocalDate;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface UpdateSignupProfileUseCase {

    Result updateProfile(Command command);

    record Command(
            UUID sessionId,     // may be null
            String name,
            String gender,
            LocalDate birthDate
    ) {}

    enum Outcome {
        UPDATED,
        NO_SESSION,
        INVALID_INPUT,
        INVALID_STATE
    }

    record Result(
            Outcome outcome,
            boolean updated,
            UUID sessionId,
            SignupSessionState state
    ) {}
}