package com.timeeconomy.auth.domain.passwordreset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth.domain.exception.InvalidPasswordResetTokenException;
import com.timeeconomy.auth.domain.passwordreset.port.in.ResetPasswordUseCase;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.in.VerifyLinkUseCase;
import com.timeeconomy.auth.domain.verification.port.in.ConsumeVerificationUseCase;

import java.time.Instant;
import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordService implements ResetPasswordUseCase {

    private final VerifyLinkUseCase verifyLinkUseCase;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final ConsumeVerificationUseCase consumeVerificationUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result resetPassword(Command command) {
        String rawToken = command.rawToken();
        String newPassword = command.newPassword();
        Instant now = Instant.now(clock);

        // 1) verify token via verification-challenge
        var verify = verifyLinkUseCase.verifyLink(
                new VerifyLinkUseCase.VerifyLinkCommand(
                        VerificationPurpose.PASSWORD_RESET,
                        VerificationChannel.EMAIL,
                        rawToken
                )
        );

        if (!verify.success() || verify.challengeId() == null) {
            throw new InvalidPasswordResetTokenException("Invalid or expired token");
        }

        // 2) identify user by destinationNorm (we stored normalized email)
        String email = verify.destinationNorm();
        AuthUser user = authUserRepositoryPort
                .findByEmail(email)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid token"));

        // 3) change password
        String newPasswordHash = passwordEncoderPort.encode(newPassword);
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(now);
        authUserRepositoryPort.save(user);

        // 4) consume token AFTER password change succeeds
        consumeVerificationUseCase.consume(new ConsumeVerificationUseCase.ConsumeCommand(verify.challengeId()));

        // 5) revoke sessions (optional but recommended)
        authSessionRepositoryPort.revokeAllByUserId(user.getId(), now);

        log.info("[PasswordReset] Password reset for userId={} email={}", user.getId(), user.getEmail());
        return new Result(true);
    }
}