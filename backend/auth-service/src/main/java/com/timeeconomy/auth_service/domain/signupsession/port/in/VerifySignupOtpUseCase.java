package com.timeeconomy.auth_service.domain.signupsession.port.in;

import com.timeeconomy.auth_service.domain.signupsession.model.SignupVerificationTarget;

import java.util.UUID;

public interface VerifySignupOtpUseCase {

    Result verify(Command command);

    record Command(
            UUID sessionId,
            SignupVerificationTarget target,
            String code
    ) {}

    record Result(
            boolean success,
            UUID sessionId,
            boolean emailVerified,
            boolean phoneVerified,
            String state
    ) {}
}