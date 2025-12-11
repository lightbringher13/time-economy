package com.timeeconomy.auth_service.domain.signupsession.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface UpdateSignupProfileUseCase {

    record Command(
            UUID sessionId,
            String email,
            String name,
            String phoneNumber,
            String gender,
            LocalDate birthDate
    ) {}

    void updateProfile(Command command);
}