package com.timeeconomy.auth_service.domain.port.in;

import java.util.UUID;

// domain.port.in
public interface SignupBootstrapUseCase {

    record Command(UUID existingSessionId) {}

    record Result(
            UUID sessionId,
            boolean exists,          // session exists and is active
            String email,
            boolean emailVerified,
            String phoneNumber,
            boolean phoneVerified,
            String name,
            String gender,
            String birthDate,
            String state              // e.g. ACTIVE / EXPIRED / etc.
    ) {}

    Result bootstrap(Command command);
}
