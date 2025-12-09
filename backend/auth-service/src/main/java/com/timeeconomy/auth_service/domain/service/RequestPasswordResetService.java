// domain.service

package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.model.PasswordResetToken;
import com.timeeconomy.auth_service.domain.port.in.RequestPasswordResetUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.PasswordResetMailPort;
import com.timeeconomy.auth_service.domain.port.out.PasswordResetTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final AuthUserRepositoryPort authUserRepositoryPort;       // already exist in your project
    private final PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort;
    private final PasswordResetMailPort passwordResetMailPort;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Result requestReset(Command command) {
        String email = command.email().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // 1) check user existence (but we won't expose this info to FE)
        Optional<?> userOpt = authUserRepositoryPort.findByEmail(email);

        if (userOpt.isEmpty()) {
            // log only; FE still gets "accepted"
            log.info("[PasswordReset] request for non-existing email={}", email);
            return new Result(true);
        }

        // 2) generate raw token
        String rawToken = generateRawToken();

        // 3) hash token for DB
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiresAt = now.plus(RESET_TOKEN_TTL);

        PasswordResetToken token = PasswordResetToken.create(
                email,
                tokenHash,
                now,
                expiresAt
        );

        passwordResetTokenRepositoryPort.save(token);

        // 4) send mail
        passwordResetMailPort.sendPasswordResetLink(email, rawToken);

        log.info("[PasswordReset] token generated for email={} expiresAt={}", email, expiresAt);

        return new Result(true);
    }

    private String generateRawToken() {
        // 32 random bytes -> Base64URL string
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash reset token", e);
        }
    }
}