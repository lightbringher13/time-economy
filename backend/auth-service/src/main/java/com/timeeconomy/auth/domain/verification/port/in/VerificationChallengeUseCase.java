package com.timeeconomy.auth.domain.verification.port.in;

import java.time.Duration;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface VerificationChallengeUseCase {

    // =========================
    // OTP (code) challenge
    // =========================

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

    CreateOtpResult createOtp(CreateOtpCommand command);

    VerifyOtpResult verifyOtp(VerifyOtpCommand command);

    // =========================
    // LINK (token) challenge
    // =========================

    record CreateLinkCommand(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,     // usually EMAIL
            String destination,              // email raw
            Duration ttl,                    // challenge TTL
            Duration tokenTtl,               // token TTL (often same as ttl)
            String linkBaseUrl,              // e.g. https://app.../reset-password
            String requestIp,
            String userAgent
    ) {}

    record CreateLinkResult(
            String challengeId,
            boolean sent,
            int ttlMinutes,
            String maskedDestination
    ) {}

    record VerifyLinkCommand(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String token   // raw token from URL
    ) {}

    record VerifyLinkResult(
            boolean success,
            String challengeId,
            String destinationNorm
    ) {}

    CreateLinkResult createLink(CreateLinkCommand command);

    VerifyLinkResult verifyLink(VerifyLinkCommand command);

    record ConsumeCommand(String challengeId) {}
    void consume(ConsumeCommand command);
}