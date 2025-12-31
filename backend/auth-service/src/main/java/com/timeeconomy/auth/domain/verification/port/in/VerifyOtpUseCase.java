package com.timeeconomy.auth.domain.verification.port.in;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface VerifyOtpUseCase {

    VerifyOtpResult verifyOtp(VerifyOtpCommand command);

    record VerifyOtpCommand(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String destination,
            String code
    ) {}

    record VerifyOtpResult(
            boolean success
    ) {}
}