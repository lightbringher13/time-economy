package com.timeeconomy.auth_service.domain.port.in;

public interface RequestEmailChangeUseCase {

    record RequestEmailChangeCommand(
            Long userId,
            String currentPassword,
            String newEmail
    ) {}

    record RequestEmailChangeResult(
            Long requestId,
            String maskedNewEmail    // e.g. "n***@gmail.com"
    ) {}

    RequestEmailChangeResult requestEmailChange(RequestEmailChangeCommand command);
}