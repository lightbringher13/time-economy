package com.timeeconomy.auth.domain.verification.port.in;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface VerifyLinkUseCase {

    VerifyLinkResult verifyLink(VerifyLinkCommand command);

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
}