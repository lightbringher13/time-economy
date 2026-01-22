package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.VerifySignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupOtpVerifierPort;
import com.timeeconomy.auth.domain.signupsession.port.out.persistence.SignupSessionStorePort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifySignupOtpService implements VerifySignupOtpUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final SignupOtpVerifierPort otpVerifierPort;   // ✅ SignupSession-owned port
    private final Clock clock;

    @Override
    @Transactional
    public Result verify(Command command) {
        final Instant now = Instant.now(clock);

        // 0) Validate input (big-co: return outcome, don’t throw)
        if (command == null || command.target() == null || command.code() == null || command.code().isBlank()) {
            return ResultBuilder.invalidInput(command == null ? null : command.sessionId());
        }

        // 1) No session id => NO_SESSION (cookie missing / expired)
        final UUID sessionId = command.sessionId();
        if (sessionId == null) {
            return ResultBuilder.noSession(null);
        }

        // 2) Load session (active only)
        final SignupSession session = signupSessionStorePort
                .findActiveById(sessionId, now)
                .orElse(null);

        if (session == null) {
            return ResultBuilder.noSession(sessionId);
        }

        // 3) Gate using FACTS (otpPending), not state
        return (command.target() == SignupVerificationTarget.EMAIL)
                ? verifyEmail(session, command.code(), now)
                : verifyPhone(session, command.code(), now);
    }

    private Result verifyEmail(SignupSession session, String code, Instant now) {
        if (!session.isEmailOtpPending()) {
            return ResultBuilder.noPendingOtp(session);
        }

        final String destination = session.getEmail();
        if (destination == null || destination.isBlank()) {
            return ResultBuilder.noPendingOtp(session);
        }

        boolean ok = otpVerifierPort.verify(new SignupOtpVerifierPort.VerifyCommand(
                session.getId().toString(),
                SignupOtpVerifierPort.Purpose.SIGNUP_EMAIL,
                SignupOtpVerifierPort.Channel.EMAIL,
                destination,
                code
        )).success();

        if (!ok) {
            return ResultBuilder.wrongCode(session);
        }

        try {
            session.confirmEmailVerified(now);
        } catch (IllegalStateException e) {
            // defensive: session facts/state changed unexpectedly
            return ResultBuilder.noPendingOtp(session);
        }

        signupSessionStorePort.save(session);
        return ResultBuilder.verified(session);
    }

    private Result verifyPhone(SignupSession session, String code, Instant now) {
        if (!session.isPhoneOtpPending()) {
            return ResultBuilder.noPendingOtp(session);
        }

        final String destination = session.getPhoneNumber();
        if (destination == null || destination.isBlank()) {
            return ResultBuilder.noPendingOtp(session);
        }

        boolean ok = otpVerifierPort.verify(new SignupOtpVerifierPort.VerifyCommand(
                session.getId().toString(),
                SignupOtpVerifierPort.Purpose.SIGNUP_PHONE,
                SignupOtpVerifierPort.Channel.SMS,
                destination,
                code
        )).success();

        if (!ok) {
            return ResultBuilder.wrongCode(session);
        }

        try {
            session.confirmPhoneVerified(now);
        } catch (IllegalStateException e) {
            return ResultBuilder.noPendingOtp(session);
        }

        signupSessionStorePort.save(session);
        return ResultBuilder.verified(session);
    }

    // ------------------------------------------------------
    // small builder to keep Result construction consistent
    // ------------------------------------------------------
    private static final class ResultBuilder {

        static Result invalidInput(UUID sessionId) {
            return new Result(
                    Outcome.INVALID_INPUT,
                    false,
                    sessionId,
                    false,
                    false,
                    false,
                    false,
                    SignupSessionState.DRAFT
            );
        }

        static Result noSession(UUID sessionId) {
            return new Result(
                    Outcome.NO_SESSION,
                    false,
                    sessionId,
                    false,
                    false,
                    false,
                    false,
                    SignupSessionState.EXPIRED
            );
        }

        static Result noPendingOtp(SignupSession s) {
            return new Result(
                    Outcome.NO_PENDING_OTP,
                    false,
                    s.getId(),
                    s.isEmailVerified(),
                    s.isPhoneVerified(),
                    s.isEmailOtpPending(),
                    s.isPhoneOtpPending(),
                    safeState(s.getState())
            );
        }

        static Result wrongCode(SignupSession s) {
            return new Result(
                    Outcome.WRONG_CODE,
                    false,
                    s.getId(),
                    s.isEmailVerified(),
                    s.isPhoneVerified(),
                    s.isEmailOtpPending(),
                    s.isPhoneOtpPending(),
                    safeState(s.getState())
            );
        }

        static Result verified(SignupSession s) {
            return new Result(
                    Outcome.VERIFIED,
                    true,
                    s.getId(),
                    s.isEmailVerified(),
                    s.isPhoneVerified(),
                    s.isEmailOtpPending(),
                    s.isPhoneOtpPending(),
                    safeState(s.getState())
            );
        }

        private static SignupSessionState safeState(SignupSessionState state) {
            return (state == null) ? SignupSessionState.DRAFT : state;
        }
    }
}