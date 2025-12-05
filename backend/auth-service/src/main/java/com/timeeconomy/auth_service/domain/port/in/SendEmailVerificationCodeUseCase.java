package com.timeeconomy.auth_service.domain.port.in;

import java.util.UUID;

public interface SendEmailVerificationCodeUseCase {

    record SendResult(String code, UUID sessionId) {}

    SendResult send(SendCommand command);

    record SendCommand(String email, UUID existingSessionId) {}
}