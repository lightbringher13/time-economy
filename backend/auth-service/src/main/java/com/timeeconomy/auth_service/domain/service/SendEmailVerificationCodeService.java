package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.model.EmailVerification;
import com.timeeconomy.auth_service.domain.model.SignupSession;
import com.timeeconomy.auth_service.domain.port.in.SendEmailVerificationCodeUseCase;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationMailPort;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.SignupSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendEmailVerificationCodeService implements SendEmailVerificationCodeUseCase {

    private static final long EXPIRES_MINUTES = 10L;
    private static final Duration SIGNUP_SESSION_TTL = Duration.ofHours(24);

    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final EmailVerificationMailPort emailVerificationMailPort;
    private final SignupSessionRepositoryPort signupSessionRepositoryPort;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public SendResult send(SendCommand command) {
        String email = command.email().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // 1) generate verification code
        String code = generateCode();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRES_MINUTES);

        // 2) save verification entry
        EmailVerification verification = new EmailVerification(email, code, expiresAt);
        emailVerificationRepositoryPort.save(verification);

        // 3) resolve or create signup session
        SignupSession session = resolveOrCreateSession(command.existingSessionId(), email, now);

        signupSessionRepositoryPort.save(session);

        // 4) send email (dev mode logs code)
        emailVerificationMailPort.sendVerificationCode(email, code);

        log.info("[EmailVerification] sent code to email={} code={} expiresAt={} sessionId={}",
                email, code, expiresAt, session.getId());

        return new SendResult(code, session.getId());
    }

    private SignupSession resolveOrCreateSession(UUID existingSessionId,
                                                 String email,
                                                 LocalDateTime now) {

        // Reuse session if cookie provided
        if (existingSessionId != null) {
            var active = signupSessionRepositoryPort.findActiveById(existingSessionId, now);
            if (active.isPresent()) {
                // Ensure email matches
                SignupSession s = active.get();
                if (!s.getEmail().equals(email)) {
                    s.updateEmail(email, now);
                }
                return s;
            }
        }

        // (Optional) reuse active session by email
        var byEmail = signupSessionRepositoryPort.findLatestActiveByEmail(email, now);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        // Create new session
        LocalDateTime expiry = now.plus(SIGNUP_SESSION_TTL);
        return SignupSession.createNew(email, now, expiry);
    }

    private String generateCode() {
        int num = random.nextInt(1_000_000);
        return String.format("%06d", num);
    }
}