package com.timeeconomy.auth_service.domain.port.in;

import java.util.UUID;

public interface VerifyPhoneCodeUseCase {

    Result verify(VerifyCommand command);

    record VerifyCommand(
            String phoneNumber,
            String code,
            UUID signupSessionId // nullable
    ) {
    }

    record Result(
            boolean success
    ) {}
}