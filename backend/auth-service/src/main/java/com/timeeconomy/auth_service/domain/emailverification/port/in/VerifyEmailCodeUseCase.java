package com.timeeconomy.auth_service.domain.emailverification.port.in;

import java.util.UUID;

public interface VerifyEmailCodeUseCase {

    VerifyResult verify(VerifyCommand command);

    record VerifyCommand(
            UUID signupSessionId, // ⭐ NEW
            String email,
            String code
    ) {}

    record VerifyResult(
            boolean success
    ) {}
}