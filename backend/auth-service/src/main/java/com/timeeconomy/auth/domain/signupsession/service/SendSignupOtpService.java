// src/main/java/com/timeeconomy/auth_service/domain/signupsession/service/SendSignupOtpService.java
package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.SendSignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.VerificationChallengeUseCase;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendSignupOtpService implements SendSignupOtpUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final SignupSessionStorePort signupSessionStorePort;
    private final VerificationChallengeUseCase verificationChallengeUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result send(Command command) {
        LocalDateTime now = LocalDateTime.now(clock);

        var sessionOpt = signupSessionStorePort.findActiveById(command.sessionId(), now);
        if (sessionOpt.isEmpty()) {
            return new Result(false, command.sessionId(), null, 0, null, false, false, "EXPIRED_OR_NOT_FOUND");
        }

        SignupSession session = sessionOpt.get();

        VerificationPurpose purpose;
        VerificationChannel channel;
        String destination;

        if (command.target() == SignupVerificationTarget.EMAIL) {
            destination = session.getEmail();
            purpose = VerificationPurpose.SIGNUP_EMAIL;
            channel = VerificationChannel.EMAIL;
        } else {
            destination = session.getPhoneNumber();
            purpose = VerificationPurpose.SIGNUP_PHONE;
            channel = VerificationChannel.SMS;
        }

        if (destination == null || destination.isBlank()) {
            return new Result(false, session.getId(), null, 0, null,
                    session.isEmailVerified(), session.isPhoneVerified(), session.getState().name());
        }

        log.info("[SignupSendOtp] sessionId={} target={} purpose={} channel={} destMasked={}",
                session.getId(), command.target(), purpose, channel, mask(channel, destination));

        var created = verificationChallengeUseCase.createOtp(new VerificationChallengeUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                session.getId().toString(),   // ✅ subjectId = sessionId (string)
                purpose,
                channel,
                destination,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null, // requestIp (controller에서 넣어줘도 됨)
                null  // userAgent
        ));

        return new Result(
                created.sent(),
                session.getId(),
                created.challengeId(),
                created.ttlMinutes(),
                created.maskedDestination(),
                session.isEmailVerified(),
                session.isPhoneVerified(),
                session.getState().name()
        );
    }

    private String mask(VerificationChannel channel, String destination) {
        if (channel == VerificationChannel.EMAIL) {
            int at = destination.indexOf('@');
            if (at <= 1) return "***" + destination.substring(Math.max(0, at));
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        if (destination.length() <= 4) return "***";
        return destination.substring(0, 3) + "****" + destination.substring(destination.length() - 2);
    }
}