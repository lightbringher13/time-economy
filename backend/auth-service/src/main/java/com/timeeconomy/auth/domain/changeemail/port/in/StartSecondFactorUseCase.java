package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

public interface StartSecondFactorUseCase {

    record StartSecondFactorCommand(
            Long userId,
            Long requestId
    ) {}

    record StartSecondFactorResult(
            Long requestId,
            SecondFactorType secondFactorType,
            EmailChangeStatus status     // SECOND_FACTOR_PENDING (or READY_TO_COMMIT/COMPLETED in idempotent cases)
    ) {}

    StartSecondFactorResult startSecondFactor(StartSecondFactorCommand command);
}