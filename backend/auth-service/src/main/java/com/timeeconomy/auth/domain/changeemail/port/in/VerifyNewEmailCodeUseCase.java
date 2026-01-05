package com.timeeconomy.auth.domain.changeemail.port.in;

public interface VerifyNewEmailCodeUseCase {

    record VerifyNewEmailCodeCommand(
            Long userId,
            Long requestId,
            String code
    ) {}

    record VerifyNewEmailCodeResult(
            Long requestId
    ) {}

    VerifyNewEmailCodeResult verifyNewEmailCode(VerifyNewEmailCodeCommand command);
}