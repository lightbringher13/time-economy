package com.timeeconomy.auth.adapter.out.signupsession.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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
    private final Clock clock;

    @Override
    public IssueResult issue(IssueCommand cmd) {
        if (cmd == null
                || isBlank(cmd.signupSessionId())
                || cmd.purpose() == null
                || cmd.channel() == null
                || isBlank(cmd.destination())) {
            return new IssueResult(Outcome.INVALID_INPUT, null, null, null, null);
        }

        VerificationPurpose purpose = switch (cmd.purpose()) {
            case SIGNUP_EMAIL -> VerificationPurpose.SIGNUP_EMAIL;
            case SIGNUP_PHONE -> VerificationPurpose.SIGNUP_PHONE;
        };

        VerificationChannel channel = switch (cmd.channel()) {
            case EMAIL -> VerificationChannel.EMAIL;
            case SMS -> VerificationChannel.SMS;
        };

        CreateOtpUseCase.CreateOtpResult created = createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                cmd.signupSessionId(),
                purpose,
                channel,
                cmd.destination(),
                cmd.requestIp(),
                cmd.userAgent()
        ));

        if (created == null) {
            return new IssueResult(Outcome.DELIVERY_FAILED, null, null, null, null);
        }

        Instant now = safeNow();

        String masked = (created.maskedDestination() == null || created.maskedDestination().isBlank())
                ? "***"
                : created.maskedDestination();

        return switch (created.outcome()) {
            case SENT -> new IssueResult(
                    Outcome.SENT,
                    created.challengeId(),
                    masked,
                    created.expiresAt(),
                    null
            );

            case THROTTLED -> new IssueResult(
                    Outcome.THROTTLED,
                    created.challengeId(), // might be existing pending id
                    masked,
                    created.expiresAt(),   // might be pending expiresAt
                    toRetryAfterSeconds(now, created.nextSendAt())
            );

            case DESTINATION_INVALID -> new IssueResult(
                    Outcome.INVALID_DESTINATION,
                    null,
                    masked,
                    null,
                    null
            );

            case INVALID_INPUT -> new IssueResult(
                    Outcome.INVALID_INPUT,
                    null,
                    masked,
                    null,
                    null
            );

            case INTERNAL_ERROR -> new IssueResult(
                    Outcome.DELIVERY_FAILED,
                    created.challengeId(),
                    masked,
                    created.expiresAt(),
                    toRetryAfterSeconds(now, created.nextSendAt())
            );
        };
    }

    private Instant safeNow() {
        try {
            return (clock != null) ? Instant.now(clock) : Instant.now();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private Integer toRetryAfterSeconds(Instant now, Instant nextSendAt) {
        if (nextSendAt == null) return null;

        Instant n = (now != null) ? now : Instant.now();
        long sec = Duration.between(n, nextSendAt).getSeconds();
        if (sec <= 0) return 0;

        return (int) Math.min(sec, 24 * 60 * 60); // 1 day cap (big-co safety)
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}