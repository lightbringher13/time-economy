// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/GetCompletedSignupSessionInfoUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.time.LocalDate;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface GetCompletedSignupSessionInfoUseCase {

    record Query(UUID sessionId) {}

    record Result(
            String email,
            String phoneNumber,
            String name,
            String gender,
            LocalDate birthDate,
            SignupSessionState state
    ) {}

    Result getCompletedInfo(Query query);
}