package com.timeeconomy.auth.domain.verification.port.in;

import java.time.Duration;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface CreateOtpUseCase {

    CreateOtpResult createOtp(CreateOtpCommand command);

    record CreateOtpCommand(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,   // EMAIL / SMS
            String destination,            // email/phone raw
            Duration ttl,
            int maxAttempts,
            String requestIp,
            String userAgent
    ) {}

    record CreateOtpResult(
            String challengeId,
            boolean sent,
            int ttlMinutes,
            String maskedDestination
    ) {}
}