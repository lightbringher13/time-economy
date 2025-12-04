package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.model.EmailVerification;
import com.timeeconomy.auth_service.domain.port.in.SendEmailVerificationCodeUseCase;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationMailPort;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationRepositoryPort;
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

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public void send(SendCommand command) {
        String email = command.email();

        // 1) 코드 생성 (000000 ~ 999999)
        String code = generateCode();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRES_MINUTES);

        // 2) 도메인 객체 생성 & 저장
        EmailVerification verification = new EmailVerification(
                email,
                code,
                expiresAt);

        emailVerificationRepositoryPort.save(verification);

        // 3) 메일 발송 (실제 구현은 adapter에서)
        emailVerificationMailPort.sendVerificationCode(email, code);

        log.info("[EmailVerification] sent code to email={} expiresAt={}", email, expiresAt);
    }

    private String generateCode() {
        int num = random.nextInt(1_000_000); // 0 ~ 999999
        return String.format("%06d", num);
    }
}