// src/main/java/com/timeeconomy/auth/domain/signupsession/port/out/integration/SignupOtpIssuerPort.java
package com.timeeconomy.auth.domain.signupsession.port.out.integration;

import java.time.Instant;

public interface SignupOtpIssuerPort {

    enum Channel { EMAIL, SMS }
    enum Purpose { SIGNUP_EMAIL, SIGNUP_PHONE }

    record IssueCommand(
            String signupSessionId,
            Purpose purpose,
            Channel channel,
            String destination,
            String requestIp,
            String userAgent
    ) {}

    enum Outcome {
        SENT,
        THROTTLED,             // cooldown/quota/provider throttle
        INVALID_DESTINATION,   // validation failed
        INVALID_INPUT,         // missing cmd fields
        DELIVERY_FAILED        // internal error / publish failure
    }

    record IssueResult(
            Outcome outcome,
            String challengeId,            // nullable unless SENT/THROTTLED (if pending exists)
            String maskedDestination,      // nullable on invalid input
            Instant expiresAt,             // nullable; if THROTTLED you may still return pending expiresAt
            Integer retryAfterSeconds      // nullable unless THROTTLED
    ) {
        public boolean sent() { return outcome == Outcome.SENT; }
        public boolean throttled() { return outcome == Outcome.THROTTLED; }
    }

    IssueResult issue(IssueCommand cmd);
}