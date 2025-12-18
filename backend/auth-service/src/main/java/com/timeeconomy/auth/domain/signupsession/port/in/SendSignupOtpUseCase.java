// src/main/java/com/timeeconomy/auth_service/domain/signupsession/port/in/SendSignupOtpUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

public interface SendSignupOtpUseCase {

    Result send(Command command);

    record Command(
            UUID sessionId,
            SignupVerificationTarget target
    ) {}

    record Result(
            boolean sent,
            UUID sessionId,
            String challengeId,
            int ttlMinutes,
            String maskedDestination,
            boolean emailVerified,
            boolean phoneVerified,
            String state
    ) {}
}