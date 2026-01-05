package com.timeeconomy.auth.domain.changeemail.port.in;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public interface VerifyNewEmailCodeUseCase {

    record VerifyNewEmailCodeCommand(Long userId, Long requestId, String code) {}

    record VerifyNewEmailCodeResult(Long requestId, EmailChangeStatus status) {}

    VerifyNewEmailCodeResult verifyNewEmailCode(VerifyNewEmailCodeCommand command);
}