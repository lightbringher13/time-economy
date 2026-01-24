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
        if (c == null
                || isBlank(c.subjectId())
                || c.purpose() == null
                || c.channel() == null
                || isBlank(c.destination())
                || isBlank(c.code())) {
            return new VerifyResult(Outcome.INVALID_INPUT, null, null, null);
        }

        VerificationPurpose purpose = switch (c.purpose()) {
            case SIGNUP_EMAIL -> VerificationPurpose.SIGNUP_EMAIL;
            case SIGNUP_PHONE -> VerificationPurpose.SIGNUP_PHONE;
        };

        VerificationChannel channel = switch (c.channel()) {
            case EMAIL -> VerificationChannel.EMAIL;
            case SMS -> VerificationChannel.SMS;
        };

        try {
            var vr = verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
                    VerificationSubjectType.SIGNUP_SESSION,
                    c.subjectId(),
                    purpose,
                    channel,
                    c.destination(),
                    c.code(),
                    c.requestIp() // ✅ NEW
            ));

            if (vr == null) {
                return new VerifyResult(Outcome.DELIVERY_FAILED, null, null, null);
            }

            Outcome out = switch (vr.outcome()) {
                case VERIFIED -> Outcome.VERIFIED;
                case WRONG_CODE -> Outcome.WRONG_CODE;
                case EXPIRED -> Outcome.EXPIRED;
                case LOCKED -> Outcome.LOCKED;
                case NO_PENDING_CHALLENGE -> Outcome.NO_PENDING_CHALLENGE;
                case DESTINATION_MISMATCH -> Outcome.DESTINATION_MISMATCH;
                case THROTTLED -> Outcome.THROTTLED;         // ✅ NEW
                case INVALID_INPUT -> Outcome.INVALID_INPUT;
                default -> Outcome.DELIVERY_FAILED;
            };

            return new VerifyResult(
                    out,
                    vr.expiresAt(),
                    vr.remainingAttempts(),
                    vr.retryAfterSeconds()
            );

        } catch (Exception e) {
            return new VerifyResult(Outcome.DELIVERY_FAILED, null, null, null);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}