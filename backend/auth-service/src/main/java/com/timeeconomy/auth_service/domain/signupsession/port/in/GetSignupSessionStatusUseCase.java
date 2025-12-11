package com.timeeconomy.auth_service.domain.signupsession.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface GetSignupSessionStatusUseCase {

    record Query(
            UUID sessionId
    ) {}

    enum SessionState {
        EMAIL_PENDING,
        EMAIL_VERIFIED,
        PROFILE_FILLED,
        COMPLETED,
        EXPIRED_OR_NOT_FOUND
    }

    record Result(
            boolean exists,
            String email,
            boolean emailVerified,
            String phoneNumber,
            boolean phoneVerified,
            String name,
            String gender,
            LocalDate birthDate,
            SessionState state
    ) {}

    Result getStatus(Query query);
}