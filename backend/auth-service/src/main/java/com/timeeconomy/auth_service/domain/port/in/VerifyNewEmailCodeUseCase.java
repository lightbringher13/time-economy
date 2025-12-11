package com.timeeconomy.auth_service.domain.port.in;

import com.timeeconomy.auth_service.domain.model.SecondFactorType;

public interface VerifyNewEmailCodeUseCase {

    record VerifyNewEmailCodeCommand(
            Long userId,
            Long requestId,
            String code
    ) {}

    record VerifyNewEmailCodeResult(
            Long requestId,
            SecondFactorType secondFactorType   // PHONE or OLD_EMAIL
    ) {}

    VerifyNewEmailCodeResult verifyNewEmailCode(VerifyNewEmailCodeCommand command);
}