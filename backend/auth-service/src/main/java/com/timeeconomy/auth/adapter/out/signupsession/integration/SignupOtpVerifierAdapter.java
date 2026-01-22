// auth/adapter/out/signupsession/integration/SignupOtpVerifierAdapter.java
package com.timeeconomy.auth.adapter.out.signupsession.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupOtpVerifierPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;

@Component
@RequiredArgsConstructor
public class SignupOtpVerifierAdapter implements SignupOtpVerifierPort {

    private final VerifyOtpUseCase verifyOtpUseCase;

    @Override
    public VerifyResult verify(VerifyCommand c) {
        if (c == null) return new VerifyResult(false);

        VerificationPurpose purpose = switch (c.purpose()) {
            case SIGNUP_EMAIL -> VerificationPurpose.SIGNUP_EMAIL;
            case SIGNUP_PHONE -> VerificationPurpose.SIGNUP_PHONE;
        };

        VerificationChannel channel = switch (c.channel()) {
            case EMAIL -> VerificationChannel.EMAIL;
            case SMS -> VerificationChannel.SMS;
        };

        boolean ok = verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                c.subjectId(),
                purpose,
                channel,
                c.destination(),
                c.code()
        )).success();

        return new VerifyResult(ok);
    }
}