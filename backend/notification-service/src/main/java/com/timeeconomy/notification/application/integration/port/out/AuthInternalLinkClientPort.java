package com.timeeconomy.notification.application.integration.port.out;

import java.util.Optional;
import java.util.UUID;

public interface AuthInternalLinkClientPort {
    Optional<String> peekLinkUrl(UUID challengeId, String purpose, UUID eventId);

    void acknowledgeLink(UUID challengeId);
}
