// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/service/AdvanceSignupSessionService.java
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
import com.timeeconomy.auth.domain.signupsession.port.in.AdvanceSignupSessionUseCase;
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
public class AdvanceSignupSessionService implements AdvanceSignupSessionUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final SignupSessionStorePort signupSessionStorePort;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final CreateOtpUseCase createOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result advance(Command command) {
        Instant now = Instant.now(clock);
        UUID sessionId = command.sessionId();

        SignupSession s = signupSessionStorePort.findActiveById(sessionId, now)
                .orElseThrow(() -> new SignupSessionNotFoundException(sessionId));

        // materialize expiry if needed
        if (s.expireIfNeeded(now)) {
            signupSessionStorePort.save(s);
            throw new SignupSessionInvalidStateException("advance", s.getState());
        }

        SignupSessionState state = s.getState();

        // ✅ big-co: "Next" is state-dependent
        if (state == SignupSessionState.DRAFT || state == SignupSessionState.EMAIL_OTP_SENT) {
            // --- Advance to EMAIL_OTP_SENT (send email OTP) ---
            String email = normalizeEmail(command.email());
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("email is required");
            }

            // save typed email
            s.setDraftEmail(email, now);

            // fast-fail: email already used?
            authUserRepositoryPort.findByEmail(email).ifPresent(u -> {
                throw new EmailAlreadyUsedException("Email is already in use");
            });

            // state transition (idempotent allowed from EMAIL_OTP_SENT)
            s.markEmailOtpSent(now);
            signupSessionStorePort.save(s);

            // side effect: send OTP (CreateOtp already cancels active pending)
            createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                    VerificationSubjectType.SIGNUP_SESSION,
                    s.getId().toString(),
                    VerificationPurpose.SIGNUP_EMAIL,
                    VerificationChannel.EMAIL,
                    s.getEmail(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));

            return toResult(s);
        }

        if (state == SignupSessionState.EMAIL_VERIFIED || state == SignupSessionState.PHONE_OTP_SENT) {
            // --- Advance to PHONE_OTP_SENT (send phone OTP) ---
            String phone = normalizePhone(command.phoneNumber());
            if (phone == null || phone.isBlank()) {
                throw new IllegalArgumentException("phoneNumber is required");
            }

            // save typed phone
            s.setDraftPhone(phone, now);

            // fast-fail: phone already used?
            authUserRepositoryPort.findByPhoneNumber(phone).ifPresent(u -> {
                throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
            });

            // state transition (idempotent allowed from PHONE_OTP_SENT)
            s.markPhoneOtpSent(now);
            signupSessionStorePort.save(s);

            // side effect: send SMS OTP
            createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                    VerificationSubjectType.SIGNUP_SESSION,
                    s.getId().toString(),
                    VerificationPurpose.SIGNUP_PHONE,
                    VerificationChannel.SMS,
                    s.getPhoneNumber(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));

            return toResult(s);
        }

        // For everything else, "advance" is not a thing.
        // PROFILE_PENDING should be advanced by submitProfile/register, not "next".
        throw new SignupSessionInvalidStateException("advance", state);
    }

    private static Result toResult(SignupSession s) {
        return new Result(
                s.getId(),
                s.getState(),
                s.getEmail(),
                s.isEmailVerified(),
                s.getPhoneNumber(),
                s.isPhoneVerified()
        );
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.trim(); // TODO: E.164 later
    }
}