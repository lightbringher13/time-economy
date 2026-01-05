package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

public interface StartSecondFactorUseCase {

    record StartSecondFactorCommand(
            Long userId,
            Long requestId
    ) {}

    record StartSecondFactorResult(
            Long requestId,
            SecondFactorType secondFactorType
    ) {}

    StartSecondFactorResult startSecondFactor(StartSecondFactorCommand command);
}