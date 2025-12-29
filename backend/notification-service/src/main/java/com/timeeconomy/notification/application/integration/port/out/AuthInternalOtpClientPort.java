package com.timeeconomy.notification.application.integration.port.out;

import java.util.Optional;
import java.util.UUID;

public interface AuthInternalOtpClientPort {
    Optional<String> getOtpOnce(UUID verificationChallengeId);
}