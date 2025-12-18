package com.timeeconomy.auth.domain.signupsession.port.in;

import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;

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