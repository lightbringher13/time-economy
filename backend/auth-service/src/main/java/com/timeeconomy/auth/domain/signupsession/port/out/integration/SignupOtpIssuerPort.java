package com.timeeconomy.auth.domain.signupsession.port.out.integration;

import java.time.Duration;

public interface SignupOtpIssuerPort {

    enum Channel { EMAIL, SMS }
    enum Purpose { SIGNUP_EMAIL, SIGNUP_PHONE }

    record IssueCommand(
            String signupSessionId,
            Purpose purpose,
            Channel channel,
            String destination,
            Duration ttl,
            int maxAttempts,
            String requestIp,
            String userAgent
    ) {}

    record IssueResult(
            boolean sent,
            String challengeId,
            int ttlMinutes,
            String maskedDestination
    ) {}

    IssueResult issue(IssueCommand cmd);
}