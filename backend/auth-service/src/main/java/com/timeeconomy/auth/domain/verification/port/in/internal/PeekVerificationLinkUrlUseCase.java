package com.timeeconomy.auth.domain.verification.port.in.internal;

import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface PeekVerificationLinkUrlUseCase {

    record Command(String challengeId, VerificationPurpose purpose) {}

    record Result(String verificationChallengeId, String linkUrl) {
        @Override
        public String toString() {
            return "Result[verificationChallengeId=" + verificationChallengeId
                    + ", linkUrl=[REDACTED]]";
        }
    }

    Result peek(Command command);
}
