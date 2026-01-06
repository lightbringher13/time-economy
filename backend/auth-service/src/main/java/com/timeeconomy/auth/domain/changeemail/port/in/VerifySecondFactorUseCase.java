package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public interface VerifySecondFactorUseCase {
    record VerifySecondFactorCommand(Long userId, Long requestId, String code) {}

    record VerifySecondFactorResult(Long requestId, EmailChangeStatus status) {}

    VerifySecondFactorResult verifySecondFactor(VerifySecondFactorCommand command);
}