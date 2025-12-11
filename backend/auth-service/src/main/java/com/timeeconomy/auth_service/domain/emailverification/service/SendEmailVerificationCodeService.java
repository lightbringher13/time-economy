package com.timeeconomy.auth_service.domain.emailverification.service;

import com.timeeconomy.auth_service.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.emailverification.model.EmailVerification;
import com.timeeconomy.auth_service.domain.emailverification.port.in.SendEmailVerificationCodeUseCase;
import com.timeeconomy.auth_service.domain.emailverification.port.out.EmailVerificationMailPort;
import com.timeeconomy.auth_service.domain.emailverification.port.out.EmailVerificationRepositoryPort;
import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;

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
    private final AuthUserRepositoryPort authUserRepositoryPort;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public SendResult send(SendCommand command) {
        String rawEmail = command.email();
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        String email = rawEmail.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        authUserRepositoryPort.findByEmail(email).ifPresent(existing -> {
            // 여기서는 "이미 가입된 이메일입니다. 로그인 또는 비밀번호 찾기를 사용하세요" 같은 메시지로 매핑 가능
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // 1) generate & save verification code
        String code = generateCode();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRES_MINUTES);

        EmailVerification verification = new EmailVerification(email, code, expiresAt);
        emailVerificationRepositoryPort.save(verification);

        // 2) send email
        emailVerificationMailPort.sendVerificationCode(email, code);

        log.info("[EmailVerification] sent code to email={} code={} expiresAt={}",
                email, code, expiresAt);

        // dev only – FE가 코드 보여주게 할 거면 유지
        return new SendResult(code);
    }

    private String generateCode() {
        int num = random.nextInt(1_000_000);
        return String.format("%06d", num);
    }
}