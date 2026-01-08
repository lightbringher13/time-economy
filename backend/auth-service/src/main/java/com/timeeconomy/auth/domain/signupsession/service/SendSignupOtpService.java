// src/main/java/com/timeeconomy/auth/domain/signupsession/service/SendSignupOtpService.java
package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.PhoneNumberAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.SendSignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendSignupOtpService implements SendSignupOtpUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final SignupSessionStorePort signupSessionStorePort;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final CreateOtpUseCase createOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result send(Command command) {
        if (command.sessionId() == null) {
            throw new SignupSessionNotFoundException("Missing signup session");
        }

        Instant now = Instant.now(clock);

        SignupSession session = signupSessionStorePort
                .findActiveById(command.sessionId(), now)
                .orElseThrow(() -> new SignupSessionNotFoundException("Signup session not found or expired"));

        // Decide verification target
        final SignupVerificationTarget target = command.target();
        if (target == null) {
            // keep behavior simple; you can introduce a dedicated exception later
            throw new IllegalArgumentException("target is required");
        }

        // -------- Idempotency: if already verified for that target, do nothing --------
        if (target == SignupVerificationTarget.EMAIL && session.isEmailVerified()) {
            return new Result(
                    false,
                    session.getId(),
                    null,
                    0,
                    null,
                    session.isEmailVerified(),
                    session.isPhoneVerified(),
                    session.getState().name()
            );
        }

        if (target == SignupVerificationTarget.PHONE && session.isPhoneVerified()) {
            return new Result(
                    false,
                    session.getId(),
                    null,
                    0,
                    null,
                    session.isEmailVerified(),
                    session.isPhoneVerified(),
                    session.getState().name()
            );
        }

        // -------- Destination + fast-fail uniqueness checks --------
        VerificationPurpose purpose;
        VerificationChannel channel;
        String destination;

        if (target == SignupVerificationTarget.EMAIL) {
            destination = normalizeEmail(session.getEmail());
            purpose = VerificationPurpose.SIGNUP_EMAIL;
            channel = VerificationChannel.EMAIL;

            if (destination == null || destination.isBlank()) {
                return new Result(false, session.getId(), null, 0, null,
                        session.isEmailVerified(), session.isPhoneVerified(), session.getState().name());
            }

            // ✅ Fast fail: email already used
            authUserRepositoryPort.findByEmail(destination).ifPresent(existing -> {
                // Optional: flip the session flag back to false to force correct UI state
                session.setEmailVerified(false);
                signupSessionStorePort.save(session);
                throw new EmailAlreadyUsedException("Email is already in use");
            });

            session.markEmailOtpSent(now);
            signupSessionStorePort.save(session);

        } else { // PHONE
            destination = normalizePhone(session.getPhoneNumber());
            purpose = VerificationPurpose.SIGNUP_PHONE;
            channel = VerificationChannel.SMS;

            if (destination == null || destination.isBlank()) {
                return new Result(false, session.getId(), null, 0, null,
                        session.isEmailVerified(), session.isPhoneVerified(), session.getState().name());
            }

            // ✅ Fast fail: phone already used
            authUserRepositoryPort.findByPhoneNumber(destination).ifPresent(existing -> {
                session.setPhoneVerified(false);
                signupSessionStorePort.save(session);
                throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
            });

            session.markPhoneOtpSent(now);
            signupSessionStorePort.save(session);
        }

        log.info("[SignupSendOtp] sessionId={} target={} purpose={} channel={} destMasked={}",
                session.getId(), target, purpose, channel, mask(channel, destination));

        var created = createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                session.getId().toString(),
                purpose,
                channel,
                destination,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null, // requestIp
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
        if (destination == null) return "***";
        if (channel == VerificationChannel.EMAIL) {
            int at = destination.indexOf('@');
            if (at <= 1) return "***" + destination.substring(Math.max(0, at));
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        if (destination.length() <= 4) return "***";
        return destination.substring(0, 3) + "****" + destination.substring(destination.length() - 2);
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }

    // keep simple; later: E.164 normalization
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.trim();
    }
}