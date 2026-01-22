// auth/adapter/out/signupsession/integration/SignupOtpChallengeCancelerAdapter.java
package com.timeeconomy.auth.adapter.out.signupsession.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupOtpChallengeCancelerPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.support.CancelOtpChallengeUseCase;

@Component
@RequiredArgsConstructor
public class SignupOtpChallengeCancelerAdapter implements SignupOtpChallengeCancelerPort {

    private final CancelOtpChallengeUseCase cancelOtpChallengeUseCase;

    @Override
    public boolean cancelPending(CancelCommand c) {
        if (c == null || c.subjectId() == null || c.subjectId().isBlank() || c.now() == null) {
            return false;
        }

        VerificationPurpose purpose = switch (c.purpose()) {
            case SIGNUP_EMAIL -> VerificationPurpose.SIGNUP_EMAIL;
            case SIGNUP_PHONE -> VerificationPurpose.SIGNUP_PHONE;
        };

        VerificationChannel channel = switch (c.channel()) {
            case EMAIL -> VerificationChannel.EMAIL;
            case SMS -> VerificationChannel.SMS;
        };

        var r = cancelOtpChallengeUseCase.cancelPending(new CancelOtpChallengeUseCase.Command(
                VerificationSubjectType.SIGNUP_SESSION,
                c.subjectId(),
                purpose,
                channel,
                c.now()
        ));

        return r.canceled();
    }
}