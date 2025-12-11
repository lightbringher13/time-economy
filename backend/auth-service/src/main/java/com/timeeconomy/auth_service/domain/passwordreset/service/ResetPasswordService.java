// domain/service/ResetPasswordService.java
package com.timeeconomy.auth_service.domain.passwordreset.service;

import com.timeeconomy.auth_service.domain.auth.model.AuthUser;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.exception.InvalidPasswordResetTokenException;
import com.timeeconomy.auth_service.domain.passwordreset.model.PasswordResetToken;
import com.timeeconomy.auth_service.domain.passwordreset.port.in.ResetPasswordUseCase;
import com.timeeconomy.auth_service.domain.passwordreset.port.out.PasswordResetTokenRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.PasswordEncoderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordService implements ResetPasswordUseCase {

    private final PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort; // optional, for logout-all

    @Override
    @Transactional
    public Result resetPassword(Command command) {
        String rawToken = command.rawToken();
        String newPassword = command.newPassword();
        LocalDateTime now = LocalDateTime.now();

        String tokenHash = hashToken(rawToken);

        // 1) Lookup token
        PasswordResetToken token = passwordResetTokenRepositoryPort
                .findValidByTokenHash(tokenHash, now)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired token"));

        // 2) Validate token (expiry & used)
        if (token.isExpired(now) || token.isUsed()) {
            throw new InvalidPasswordResetTokenException("Invalid or expired token");
        }

        // 3) Find AuthUser by email from token
        AuthUser user = authUserRepositoryPort
                .findByEmail(token.getEmail())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid token"));

        // 4) Update user password
        String newPasswordHash = passwordEncoderPort.encode(newPassword);
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(now);
        authUserRepositoryPort.save(user);

        // 5) Mark token used
        token.markUsed(now);
        passwordResetTokenRepositoryPort.save(token);

        // 6) Optional: revoke all refresh tokens for this user (logout everywhere)
        authSessionRepositoryPort.revokeAllByUserId(user.getId(), now);

        log.info("[PasswordReset] Password reset for userId={} email={}", user.getId(), user.getEmail());

        return new Result(true);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}