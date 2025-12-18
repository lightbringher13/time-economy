// src/main/java/com/timeeconomy/auth_service/domain/signupsession/port/in/SendSignupOtpUseCase.java
package com.timeeconomy.auth_service.domain.signupsession.port.in;

import com.timeeconomy.auth_service.domain.signupsession.model.SignupVerificationTarget;

import java.util.UUID;

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