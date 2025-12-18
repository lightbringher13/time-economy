package com.timeeconomy.auth.domain.changeemail.port.in;

public interface VerifySecondFactorUseCase {

    record VerifySecondFactorCommand(
            Long userId,
            Long requestId,
            String code
    ) {}

    record VerifySecondFactorResult(
            Long requestId,
            String newEmail
    ) {}

    VerifySecondFactorResult verifySecondFactorAndCommit(VerifySecondFactorCommand command);
}