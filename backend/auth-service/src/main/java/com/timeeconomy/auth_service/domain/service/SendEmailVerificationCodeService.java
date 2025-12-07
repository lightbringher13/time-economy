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

@Service
@RequiredArgsConstructor
@Slf4j
public class SendEmailVerificationCodeService implements SendEmailVerificationCodeUseCase {

    private static final long EXPIRES_MINUTES = 10L;

    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final EmailVerificationMailPort emailVerificationMailPort;
    private final SignupSessionRepositoryPort signupSessionRepositoryPort;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public SendResult send(SendCommand command) {
        String email = command.email().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // 1) generate & save verification code
        String code = generateCode();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRES_MINUTES);

        EmailVerification verification = new EmailVerification(email, code, expiresAt);
        emailVerificationRepositoryPort.save(verification);

        // 2) find existing signup session (MUST exist)
        SignupSession session = signupSessionRepositoryPort
                .findActiveById(command.signupSessionId(), now)
                .orElseThrow(() ->
                        new IllegalStateException("Signup session not initialized"));

        // 3) keep email in session in sync (user might edit email)
        if (!email.equals(session.getEmail())) {
            session.updateEmail(email, now);   // or a setter/domain method
            signupSessionRepositoryPort.save(session);
        }

        // 4) send email via adapter
        emailVerificationMailPort.sendVerificationCode(email, code);

        log.info("[EmailVerification] sent code to email={} code={} expiresAt={} sessionId={}",
                email, code, expiresAt, session.getId());

        // dev-only: we still return code
        return new SendResult(code);
    }

    private String generateCode() {
        int num = random.nextInt(1_000_000);
        return String.format("%06d", num);
    }
}