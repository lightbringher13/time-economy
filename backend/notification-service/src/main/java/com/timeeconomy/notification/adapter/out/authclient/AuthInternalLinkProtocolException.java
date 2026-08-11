package com.timeeconomy.notification.adapter.out.authclient;

import java.util.UUID;

public final class AuthInternalLinkProtocolException extends RuntimeException {

    public AuthInternalLinkProtocolException(UUID challengeId, String reason) {
        super("Invalid Auth internal link response for challengeId=" + challengeId + ": " + reason);
    }
}
