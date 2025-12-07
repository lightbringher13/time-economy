package com.timeeconomy.auth_service.domain.port.in;

import java.util.UUID;

public interface SendEmailVerificationCodeUseCase {

    record SendCommand(
            String email,
            UUID signupSessionId   // must already exist (from cookie)
    ) {}

    record SendResult(
            String code            // only for dev, can remove later
    ) {}

    SendResult send(SendCommand command);
}