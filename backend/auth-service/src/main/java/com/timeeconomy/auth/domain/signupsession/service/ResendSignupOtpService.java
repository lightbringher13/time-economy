// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/service/ResendSignupOtpService.java
package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.PhoneNumberAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;

import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionInvalidStateException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.ResendSignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResendSignupOtpService implements ResendSignupOtpUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final SignupSessionStorePort signupSessionStorePort;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final CreateOtpUseCase createOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result resend(Command command) {
        final Instant now = Instant.now(clock);
        final UUID sessionId = command.sessionId();

        SignupSession session = signupSessionStorePort
                .findById(sessionId)
                .orElseThrow(() -> new SignupSessionNotFoundException(sessionId));

        // materialize expiry
        if (session.expireIfNeeded(now)) {
            signupSessionStorePort.save(session);
            throw new SignupSessionNotFoundException(sessionId); // or dedicated SignupSessionExpiredException
        }

        if (session.isTerminal()) {
            throw new SignupSessionInvalidStateException("resend otp", session.getState());
        }

        if (command.target() == SignupVerificationTarget.EMAIL) {
            return resendEmail(session, now);
        } else {
            return resendPhone(session, now);
        }
    }

    private Result resendEmail(SignupSession session, Instant now) {
        // ✅ recommend: only allow resend when already in EMAIL_OTP_SENT
        if (session.getState() != SignupSessionState.EMAIL_OTP_SENT) {
            throw new SignupSessionInvalidStateException("resend email otp", session.getState());
        }

        String email = session.getEmail();
        if (email == null || email.isBlank()) {
            throw new SignupSessionInvalidStateException("resend email otp (email missing)", session.getState());
        }

        String norm = normalizeEmail(email);

        // ✅ fast-fail: already used
        authUserRepositoryPort.findByEmail(norm).ifPresent(u -> {
            // you can also reset session flags/state if you want, but minimal is just throw
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // keep session consistent (also refresh updatedAt)
        session.markEmailOtpSent(now);
        signupSessionStorePort.save(session);

        var created = createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                session.getId().toString(),
                VerificationPurpose.SIGNUP_EMAIL,
                VerificationChannel.EMAIL,
                norm,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null,
                null
        ));

        return new Result(
                session.getId(),
                created.sent(),
                created.maskedDestination(),
                created.ttlMinutes(),
                session.getState()
        );
    }

    private Result resendPhone(SignupSession session, Instant now) {
        // ✅ recommend: only allow resend when already in PHONE_OTP_SENT
        if (session.getState() != SignupSessionState.PHONE_OTP_SENT) {
            throw new SignupSessionInvalidStateException("resend phone otp", session.getState());
        }

        String phone = session.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            throw new SignupSessionInvalidStateException("resend phone otp (phone missing)", session.getState());
        }

        // ✅ fast-fail: already used
        authUserRepositoryPort.findByPhoneNumber(phone).ifPresent(u -> {
            throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
        });

        session.markPhoneOtpSent(now);
        signupSessionStorePort.save(session);

        var created = createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                session.getId().toString(),
                VerificationPurpose.SIGNUP_PHONE,
                VerificationChannel.SMS,
                phone,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null,
                null
        ));

        return new Result(
                session.getId(),
                created.sent(),
                created.maskedDestination(),
                created.ttlMinutes(),
                session.getState()
        );
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }
}