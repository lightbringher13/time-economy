package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionInvalidStateException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.VerifySignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifySignupOtpService implements VerifySignupOtpUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result verify(Command command) {
        final Instant now = Instant.now(clock);
        final UUID sessionId = command.sessionId();

        SignupSession session = signupSessionStorePort
                .findActiveById(sessionId, now)
                .orElseThrow(() -> new SignupSessionNotFoundException(sessionId));

        // materialize expiry (if your store can return non-expired only, this is optional)
        if (session.expireIfNeeded(now)) {
            signupSessionStorePort.save(session);
            throw new SignupSessionNotFoundException(sessionId);
        }

        if (command.target() == SignupVerificationTarget.EMAIL) {
            return verifyEmailOtp(command, session, now);
        }

        return verifyPhoneOtp(command, session, now);
    }

    private Result verifyEmailOtp(Command command, SignupSession session, Instant now) {
        // ✅ allow email verify only after EMAIL_OTP_SENT
        if (session.getState() != SignupSessionState.EMAIL_OTP_SENT) {
            throw new SignupSessionInvalidStateException("verify EMAIL otp", session.getState());
        }

        String destination = session.getEmail();
        if (destination == null || destination.isBlank()) {
            throw new SignupSessionInvalidStateException("verify EMAIL otp (email missing)", session.getState());
        }

        boolean ok = verifyOtpUseCase.verifyOtp(
                new VerifyOtpUseCase.VerifyOtpCommand(
                        VerificationSubjectType.SIGNUP_SESSION,
                        session.getId().toString(),
                        VerificationPurpose.SIGNUP_EMAIL,
                        VerificationChannel.EMAIL,
                        destination,
                        command.code()
                )
        ).success();

        if (!ok) {
            // wrong code is NOT exceptional
            return fail(session);
        }

        session.markEmailVerified(now);
        signupSessionStorePort.save(session);

        return success(session);
    }

    private Result verifyPhoneOtp(Command command, SignupSession session, Instant now) {
        // ✅ allow phone verify only after PHONE_OTP_SENT
        if (session.getState() != SignupSessionState.PHONE_OTP_SENT) {
            throw new SignupSessionInvalidStateException("verify PHONE otp", session.getState());
        }

        String destination = session.getPhoneNumber();
        if (destination == null || destination.isBlank()) {
            throw new SignupSessionInvalidStateException("verify PHONE otp (phone missing)", session.getState());
        }

        boolean ok = verifyOtpUseCase.verifyOtp(
                new VerifyOtpUseCase.VerifyOtpCommand(
                        VerificationSubjectType.SIGNUP_SESSION,
                        session.getId().toString(),
                        VerificationPurpose.SIGNUP_PHONE,
                        VerificationChannel.SMS,
                        destination,
                        command.code()
                )
        ).success();

        if (!ok) {
            return fail(session);
        }

        session.markPhoneVerified(now);
        signupSessionStorePort.save(session);

        return success(session);
    }

    private Result success(SignupSession session) {
        return new Result(
                true,
                session.getId(),
                session.isEmailVerified(),
                session.isPhoneVerified(),
                safe(session.getState())
        );
    }

    private Result fail(SignupSession session) {
        return new Result(
                false,
                session.getId(),
                session.isEmailVerified(),
                session.isPhoneVerified(),
                safe(session.getState())
        );
    }

    private static String safe(Enum<?> e) {
        return e == null ? "NULL" : e.name();
    }
}