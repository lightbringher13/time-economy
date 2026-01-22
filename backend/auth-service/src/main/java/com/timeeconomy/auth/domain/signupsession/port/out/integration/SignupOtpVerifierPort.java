// auth/domain/signupsession/port/out/integration/SignupOtpVerifierPort.java
package com.timeeconomy.auth.domain.signupsession.port.out.integration;

public interface SignupOtpVerifierPort {

    enum Purpose {
        SIGNUP_EMAIL,
        SIGNUP_PHONE
    }

    enum Channel {
        EMAIL,
        SMS
    }

    record VerifyCommand(
            String subjectId,      // signupSessionId (string)
            Purpose purpose,
            Channel channel,
            String destination,    // email/phone stored in session
            String code
    ) {}

    record VerifyResult(
            boolean success
    ) {}

    VerifyResult verify(VerifyCommand command);
}