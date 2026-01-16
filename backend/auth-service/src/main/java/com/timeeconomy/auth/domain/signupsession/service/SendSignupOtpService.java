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
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
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
import java.util.Objects;
import java.util.UUID;

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
        Objects.requireNonNull(command, "command is required");

        final Instant now = Instant.now(clock);

        final SignupVerificationTarget target = requireTarget(command.target());
        final String destination = normalizeDestination(target, command.destination());

        if (destination == null) {
            // ✅ big-co: don’t throw, return typed outcome
            return resultForInvalidDestination(command.sessionId());
        }

        // 1) Fast-fail uniqueness BEFORE touching session
        if (target == SignupVerificationTarget.EMAIL) {
            authUserRepositoryPort.findByEmail(destination).ifPresent(u -> {
                throw new EmailAlreadyUsedException("Email is already in use");
            });
        } else {
            authUserRepositoryPort.findByPhoneNumber(destination).ifPresent(u -> {
                throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
            });
        }

        // 2) Load or create session (lazy-open)
        final LoadOrCreate loaded = loadOrCreateSession(command.sessionId(), now);
        final SignupSession session = loaded.session();
        final boolean sessionCreated = loaded.created();

        // 3) Idempotency: already verified AND destination matches => no-op
        if (target == SignupVerificationTarget.EMAIL
                && session.isEmailVerified()
                && destination.equals(normalizeEmail(session.getEmail()))) {
            return new Result(
                    Outcome.ALREADY_VERIFIED,
                    false,
                    session.getId(),
                    sessionCreated,
                    null,
                    0,
                    null,
                    session.isEmailVerified(),
                    session.isPhoneVerified(),
                    session.isEmailOtpPending(),
                    session.isPhoneOtpPending(),
                    session.getState()
            );
        }

        if (target == SignupVerificationTarget.PHONE
                && session.isPhoneVerified()
                && destination.equals(normalizePhone(session.getPhoneNumber()))) {
            return new Result(
                    Outcome.ALREADY_VERIFIED,
                    false,
                    session.getId(),
                    sessionCreated,
                    null,
                    0,
                    null,
                    session.isEmailVerified(),
                    session.isPhoneVerified(),
                    session.isEmailOtpPending(),
                    session.isPhoneOtpPending(),
                    session.getState()
            );
        }

        // 4) Resolve verification parameters
        final VerificationPurpose purpose =
                (target == SignupVerificationTarget.EMAIL)
                        ? VerificationPurpose.SIGNUP_EMAIL
                        : VerificationPurpose.SIGNUP_PHONE;

        final VerificationChannel channel =
                (target == SignupVerificationTarget.EMAIL)
                        ? VerificationChannel.EMAIL
                        : VerificationChannel.SMS;

        log.info("[SignupSendOtp] sessionId={} created={} target={} purpose={} channel={} destMasked={} ip={} uaPresent={}",
                session.getId(),
                sessionCreated,
                target,
                purpose,
                channel,
                mask(channel, destination),
                safe(command.requestIp()),
                command.userAgent() != null && !command.userAgent().isBlank()
        );

        // 5) Create OTP FIRST (if this fails, do not set otpPending/state)
        var createdOtp = createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.SIGNUP_SESSION,
                session.getId().toString(),
                purpose,
                channel,
                destination,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                command.requestIp(),
                command.userAgent()
        ));

        // 6) Apply domain command AFTER OTP creation
        if (target == SignupVerificationTarget.EMAIL) {
            session.requestEmailOtp(destination, now);
        } else {
            session.requestPhoneOtp(destination, now);
        }

        // 7) Persist facts/state
        signupSessionStorePort.save(session);

        return new Result(
                Outcome.SENT,
                createdOtp.sent(),
                session.getId(),
                sessionCreated,
                createdOtp.challengeId(),
                createdOtp.ttlMinutes(),
                createdOtp.maskedDestination(),
                session.isEmailVerified(),
                session.isPhoneVerified(),
                session.isEmailOtpPending(),
                session.isPhoneOtpPending(),
                session.getState()
        );
    }

    // ---------------------------
    // helpers
    // ---------------------------

    private Result resultForInvalidDestination(UUID sessionId) {
        // if sessionId existed, still return it; else null (no session created for invalid input)
        return new Result(
                Outcome.INVALID_DESTINATION,
                false,
                sessionId,
                false,
                null,
                0,
                null,
                false,
                false,
                false,
                false,
                SignupSessionState.DRAFT
        );
    }

    private record LoadOrCreate(SignupSession session, boolean created) {}

    private LoadOrCreate loadOrCreateSession(UUID sessionId, Instant now) {
        if (sessionId == null) {
            SignupSession created = signupSessionStorePort.createNew(now);
            return new LoadOrCreate(created, true);
        }

        SignupSession existing = signupSessionStorePort
                .findActiveById(sessionId, now)
                .orElseThrow(() -> new SignupSessionNotFoundException("Signup session not found or expired"));

        return new LoadOrCreate(existing, false);
    }

    private SignupVerificationTarget requireTarget(SignupVerificationTarget target) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return target;
    }

    private String normalizeDestination(SignupVerificationTarget target, String raw) {
        if (raw == null || raw.isBlank()) return null;
        return (target == SignupVerificationTarget.EMAIL) ? normalizeEmail(raw) : normalizePhone(raw);
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
        return raw == null ? null : raw.trim().toLowerCase();
    }

    private String normalizePhone(String raw) {
        return raw == null ? null : raw.trim();
    }

    private String safe(String v) {
        return (v == null) ? "-" : v;
    }
}