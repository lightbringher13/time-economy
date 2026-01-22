package com.timeeconomy.auth.adapter.out.signupsession.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupOtpIssuerPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;

@Component
@RequiredArgsConstructor
public class SignupOtpIssuerAdapter implements SignupOtpIssuerPort {

    private final CreateOtpUseCase createOtpUseCase;

    @Override
    public IssueResult issue(IssueCommand cmd) {
        VerificationPurpose purpose = switch (cmd.purpose()) {
            case SIGNUP_EMAIL -> VerificationPurpose.SIGNUP_EMAIL;
            case SIGNUP_PHONE -> VerificationPurpose.SIGNUP_PHONE;
        };

        VerificationChannel channel = switch (cmd.channel()) {
            case EMAIL -> VerificationChannel.EMAIL;
            case SMS -> VerificationChannel.SMS;
        };

        var created = createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                cmd.signupSessionId(),
                purpose,
                channel,
                cmd.destination(),
                cmd.ttl(),
                cmd.maxAttempts(),
                cmd.requestIp(),
                cmd.userAgent()
        ));

        return new IssueResult(
                created.sent(),
                created.challengeId(),
                created.ttlMinutes(),
                created.maskedDestination()
        );
    }
}