package com.timeeconomy.auth.domain.changeemail.port.in;

public interface CommitEmailChangeUseCase {

    record CommitEmailChangeCommand(
            Long userId,
            Long requestId
    ) {}

    record CommitEmailChangeResult(
            Long requestId,
            String newEmail
    ) {}

    CommitEmailChangeResult commit(CommitEmailChangeCommand command);
}
