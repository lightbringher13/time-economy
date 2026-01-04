package com.timeeconomy.auth.domain.verification.port.in.internal;

public interface GetVerificationOtpOnceUseCase {
    record Command(String challengeId) {}
    record Result(String verificationChallengeId, String otp) {}
    Result getOnce(Command command);
}