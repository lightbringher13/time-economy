package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public interface RequestEmailChangeUseCase {

    record RequestEmailChangeCommand(
            Long userId,
            String currentPassword,
            String newEmail
    ) {}

    record RequestEmailChangeResult(
            Long requestId,
            String maskedNewEmail,      // e.g. "n***@gmail.com"
            EmailChangeStatus status    // usually PENDING
    ) {}

    RequestEmailChangeResult requestEmailChange(RequestEmailChangeCommand command);
}