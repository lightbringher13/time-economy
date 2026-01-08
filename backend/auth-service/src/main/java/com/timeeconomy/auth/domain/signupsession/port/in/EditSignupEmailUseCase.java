package com.timeeconomy.auth.domain.signupsession.port.in;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

import java.time.LocalDate;
import java.util.UUID;

public interface EditSignupEmailUseCase {

    record Command(UUID sessionId, String newEmail) {}

    record Result(
            UUID sessionId,
            String email,
            boolean emailVerified,
            String phoneNumber,
            boolean phoneVerified,
            String name,
            String gender,
            LocalDate birthDate,
            SignupSessionState state
    ) {}

    Result editEmail(Command command);
}