package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.EmailVerificationAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationExpiredException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationNotFoundException;
import com.timeeconomy.auth_service.domain.model.EmailVerification;
import com.timeeconomy.auth_service.domain.port.in.VerifyEmailCodeUseCase;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailCodeService implements VerifyEmailCodeUseCase {

    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;

    @Override
    @Transactional
    public VerifyResult verify(VerifyCommand command) {
        String email = command.email();
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

        log.info("[EmailVerification] verified email={} code={}", email, inputCode);

        return new VerifyResult(true);
    }
}