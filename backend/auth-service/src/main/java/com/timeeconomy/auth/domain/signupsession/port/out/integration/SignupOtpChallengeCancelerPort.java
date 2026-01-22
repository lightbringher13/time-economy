// auth/domain/signupsession/port/out/integration/SignupOtpChallengeCancelerPort.java
package com.timeeconomy.auth.domain.signupsession.port.out.integration;

import java.time.Instant;

public interface SignupOtpChallengeCancelerPort {

    enum Purpose {
        SIGNUP_EMAIL,
        SIGNUP_PHONE
    }

    enum Channel {
        EMAIL,
        SMS
    }

    record CancelCommand(
            String subjectId,   // signupSessionId as string
            Purpose purpose,
            Channel channel,
            Instant now
    ) {}

    /**
     * Best-effort: cancels a pending challenge if present.
     * @return true if something was canceled, false otherwise
     */
    boolean cancelPending(CancelCommand command);
}