package com.timeeconomy.notification.adapter.out.authclient;

import java.util.UUID;

public final class AuthInternalOtpProtocolException extends RuntimeException {

    public AuthInternalOtpProtocolException(UUID challengeId, String reason) {
        super("Invalid Auth internal OTP response for challengeId=" + challengeId + ": " + reason);
    }
}
