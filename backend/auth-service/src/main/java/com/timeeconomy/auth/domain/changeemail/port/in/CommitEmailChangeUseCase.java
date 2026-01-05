package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public interface CommitEmailChangeUseCase {

    record CommitEmailChangeCommand(
            Long userId,
            Long requestId
    ) {}

    record CommitEmailChangeResult(
            Long requestId,
            String newEmail,
            EmailChangeStatus status
    ) {}

    CommitEmailChangeResult commit(CommitEmailChangeCommand command);
}