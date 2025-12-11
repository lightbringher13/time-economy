package com.timeeconomy.auth_service.domain.emailverification.service;

import com.timeeconomy.auth_service.domain.emailverification.model.EmailVerification;
import com.timeeconomy.auth_service.domain.emailverification.port.in.VerifyEmailCodeUseCase;
import com.timeeconomy.auth_service.domain.emailverification.port.out.EmailVerificationRepositoryPort;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationExpiredException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationNotFoundException;
import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth_service.domain.signupsession.port.out.SignupSessionRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailCodeService implements VerifyEmailCodeUseCase {

    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final SignupSessionRepositoryPort signupSessionRepositoryPort;

    @Override
    @Transactional
    public VerifyResult verify(VerifyCommand command) {
        String email = normalizeEmail(command.email());
        String inputCode = command.code();
        LocalDateTime now = LocalDateTime.now();

        // ⭐ 1) Always fetch the most recent verification record for this email
        EmailVerification latest = emailVerificationRepositoryPort
                .findLatestByEmail(email)
                .orElseThrow(() ->
                        new EmailVerificationNotFoundException("No verification code found for email"));

        // 2) Already used?
        if (latest.isVerified()) {
            throw new EmailVerificationAlreadyUsedException("Verification code already used");
        }

        // 3) Expired?
        if (latest.isExpired(now)) {
            throw new EmailVerificationExpiredException("Verification code expired");
        }

        // ⭐ 4) Code must match the *latest* code only
        if (!latest.getCode().equals(inputCode)) {
            // old code or wrong code → all fail here
            throw new EmailVerificationNotFoundException("Invalid verification code for email");
        }

        // 5) Success → mark as verified and save
        latest.markVerified(now);
        emailVerificationRepositoryPort.save(latest);

        // 6) 🔗 Link to signup session (if available)
        linkToSignupSession(command.signupSessionId(), email, now);

        log.info("[EmailVerification] verified email={} code={}", email, inputCode);

        return new VerifyResult(true);
    }

    // ----------------- helpers only for signup session -----------------

    private void linkToSignupSession(UUID signupSessionId, String email, LocalDateTime now) {
        if (signupSessionId != null) {
            linkToSpecificSignupSession(signupSessionId, email, now);
        } else {
            linkToLatestSignupSessionByEmail(email, now);
        }
    }

    private void linkToSpecificSignupSession(UUID signupSessionId, String email, LocalDateTime now) {
        SignupSession session = signupSessionRepositoryPort
                .findActiveById(signupSessionId, now)
                .orElseThrow(() ->
                        new SignupSessionNotFoundException("Active signup session not found: " + signupSessionId));

        updateSessionEmailIfNeeded(session, email, now);
        session.markEmailVerified(now);
        signupSessionRepositoryPort.save(session);

        log.info("[EmailVerification] verified and linked to signupSessionId={} email={}",
                session.getId(), email);
    }

    private void linkToLatestSignupSessionByEmail(String email, LocalDateTime now) {
        signupSessionRepositoryPort.findLatestActiveByEmail(email, now)
                .ifPresent(session -> {
                    updateSessionEmailIfNeeded(session, email, now);
                    session.markEmailVerified(now);
                    signupSessionRepositoryPort.save(session);

                    log.info("[EmailVerification] verified and linked to latest active signupSessionId={} email={}",
                            session.getId(), email);
                });
    }

    private void updateSessionEmailIfNeeded(SignupSession session, String email, LocalDateTime now) {
        if (session.getEmail() == null || !session.getEmail().equalsIgnoreCase(email)) {
            session.updateEmail(email, now); // your existing domain method
        }
    }

    // ----------------- existing helper -----------------

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }
}