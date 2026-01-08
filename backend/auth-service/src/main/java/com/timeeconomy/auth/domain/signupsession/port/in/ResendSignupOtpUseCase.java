// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/ResendSignupOtpUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface ResendSignupOtpUseCase {

    record Command(
            UUID sessionId,
            SignupVerificationTarget target
    ) {}

    record Result(
            UUID sessionId,
            boolean sent,
            String maskedDestination,
            int ttlMinutes,
            SignupSessionState state
    ) {}

    Result resend(Command command);
}