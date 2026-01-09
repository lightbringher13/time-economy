package com.timeeconomy.auth.domain.signupsession.port.in;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

import java.time.LocalDate;
import java.util.UUID;

public interface EditSignupPhoneUseCase {

    record Command(UUID sessionId, String newPhoneNumber) {}

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

    Result editPhone(Command command);
}