package com.timeeconomy.notification.application.integration.port.out;

import java.util.Optional;
import java.util.UUID;

public interface AuthInternalLinkClientPort {
    Optional<String> getLinkUrlOnce(UUID verificationChallengeId, String purpose, UUID eventId);
}