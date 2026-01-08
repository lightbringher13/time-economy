// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/AdvanceSignupSessionUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface AdvanceSignupSessionUseCase {

    record Command(
            UUID sessionId,
            String email,        // used when advancing from DRAFT -> EMAIL_OTP_SENT
            String phoneNumber   // used when advancing from EMAIL_VERIFIED -> PHONE_OTP_SENT
    ) {}

    record Result(
            UUID sessionId,
            SignupSessionState state,
            String email,
            boolean emailVerified,
            String phoneNumber,
            boolean phoneVerified
    ) {}

    Result advance(Command command);
}