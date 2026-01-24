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
    private final SignupOtpVerifierPort otpVerifierPort;
    private final Clock clock;

    @Override
    @Transactional
    public Result verify(Command command) {
        final Instant now = Instant.now(clock);

        if (command == null || command.target() == null || isBlank(command.code())) {
            return ResultBuilder.invalidInput(command == null ? null : command.sessionId());
        }

        final UUID sessionId = command.sessionId();
        if (sessionId == null) {
            return ResultBuilder.noSession(null);
        }

        final SignupSession session = signupSessionStorePort
                .findActiveById(sessionId, now)
                .orElse(null);

        if (session == null) {
            return ResultBuilder.noSession(sessionId);
        }

        return (command.target() == SignupVerificationTarget.EMAIL)
                ? verifyEmail(session, command.code(), command.requestIp(), now)
                : verifyPhone(session, command.code(), command.requestIp(), now);
    }

    private Result verifyEmail(SignupSession session, String code, String requestIp, Instant now) {
        if (!session.isEmailOtpPending() || isBlank(session.getEmail())) {
            return ResultBuilder.noPendingOtp(session);
        }

        SignupOtpVerifierPort.VerifyResult vr = otpVerifierPort.verify(
                new SignupOtpVerifierPort.VerifyCommand(
                        session.getId().toString(),
                        SignupOtpVerifierPort.Purpose.SIGNUP_EMAIL,
                        SignupOtpVerifierPort.Channel.EMAIL,
                        session.getEmail(),
                        code,
                        requestIp
                )
        );

        return handleVerifierResult(session, vr, true, now);
    }

    private Result verifyPhone(SignupSession session, String code, String requestIp, Instant now) {
        if (!session.isPhoneOtpPending() || isBlank(session.getPhoneNumber())) {
            return ResultBuilder.noPendingOtp(session);
        }

        SignupOtpVerifierPort.VerifyResult vr = otpVerifierPort.verify(
                new SignupOtpVerifierPort.VerifyCommand(
                        session.getId().toString(),
                        SignupOtpVerifierPort.Purpose.SIGNUP_PHONE,
                        SignupOtpVerifierPort.Channel.SMS,
                        session.getPhoneNumber(),
                        code,
                        requestIp
                )
        );

        return handleVerifierResult(session, vr, false, now);
    }

    private Result handleVerifierResult(SignupSession session,
                                        SignupOtpVerifierPort.VerifyResult vr,
                                        boolean isEmail,
                                        Instant now) {

        if (vr == null) {
            // conservative bucket
            return ResultBuilder.fromSession(session, Outcome.WRONG_CODE, false, null, null, null);
        }

        Outcome out = switch (vr.outcome()) {
            case VERIFIED -> Outcome.VERIFIED;
            case WRONG_CODE -> Outcome.WRONG_CODE;
            case EXPIRED -> Outcome.EXPIRED;
            case LOCKED -> Outcome.LOCKED;
            case THROTTLED -> Outcome.THROTTLED;

            // verification says "no usable pending challenge" -> session-level NO_PENDING_OTP
            case NO_PENDING_CHALLENGE, DESTINATION_MISMATCH -> Outcome.NO_PENDING_OTP;

            // safe default buckets
            case INVALID_INPUT, DELIVERY_FAILED -> Outcome.WRONG_CODE;
        };

        // not verified => no session mutation
        if (out != Outcome.VERIFIED) {
            return ResultBuilder.fromSession(
                    session,
                    out,
                    false,
                    vr.expiresAt(),
                    vr.remainingAttempts(),
                    vr.retryAfterSeconds()
            );
        }

        // verified => advance session facts
        try {
            if (isEmail) session.confirmEmailVerified(now);
            else session.confirmPhoneVerified(now);
        } catch (IllegalStateException e) {
            return ResultBuilder.fromSession(
                    session,
                    Outcome.NO_PENDING_OTP,
                    false,
                    vr.expiresAt(),
                    vr.remainingAttempts(),
                    vr.retryAfterSeconds()
            );
        }

        signupSessionStorePort.save(session);

        return ResultBuilder.fromSession(
                session,
                Outcome.VERIFIED,
                true,
                vr.expiresAt(),
                vr.remainingAttempts(),
                vr.retryAfterSeconds()
        );
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static final class ResultBuilder {

        static Result invalidInput(UUID sessionId) {
            return new Result(
                    Outcome.INVALID_INPUT,
                    false,
                    sessionId,
                    null, null, null,
                    false, false, false, false,
                    SignupSessionState.DRAFT
            );
        }

        static Result noSession(UUID sessionId) {
            return new Result(
                    Outcome.NO_SESSION,
                    false,
                    sessionId,
                    null, null, null,
                    false, false, false, false,
                    SignupSessionState.EXPIRED
            );
        }

        static Result noPendingOtp(SignupSession s) {
            return fromSession(s, Outcome.NO_PENDING_OTP, false, null, null, null);
        }

        static Result fromSession(SignupSession s,
                                 Outcome outcome,
                                 boolean success,
                                 Instant otpExpiresAt,
                                 Integer remainingAttempts,
                                 Integer retryAfterSeconds) {
            return new Result(
                    outcome,
                    success,
                    s.getId(),
                    otpExpiresAt,
                    remainingAttempts,
                    retryAfterSeconds,
                    s.isEmailVerified(),
                    s.isPhoneVerified(),
                    s.isEmailOtpPending(),
                    s.isPhoneOtpPending(),
                    (s.getState() == null ? SignupSessionState.DRAFT : s.getState())
            );
        }
    }
}