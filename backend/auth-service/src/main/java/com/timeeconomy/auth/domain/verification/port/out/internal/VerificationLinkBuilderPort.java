package com.timeeconomy.auth.domain.verification.port.out.internal;

import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface VerificationLinkBuilderPort {
    String buildLinkUrl(String rawToken, VerificationPurpose purpose);
}
