package com.timeeconomy.auth.domain.verification.port.in.internal;

import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface GetVerificationLinkUrlOnceUseCase {
    record Command(String challengeId, VerificationPurpose purpose) {}
    record Result(String verificationChallengeId, String linkUrl) {}
    Result getOnce(Command command);
}
