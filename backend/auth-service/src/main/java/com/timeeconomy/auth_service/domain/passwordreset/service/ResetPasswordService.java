package com.timeeconomy.auth_service.domain.passwordreset.service;

import com.timeeconomy.auth_service.domain.auth.model.AuthUser;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth_service.domain.exception.InvalidPasswordResetTokenException;
import com.timeeconomy.auth_service.domain.passwordreset.port.in.ResetPasswordUseCase;
import com.timeeconomy.auth_service.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth_service.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth_service.domain.verification.port.in.VerificationChallengeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordService implements ResetPasswordUseCase {

    private final VerificationChallengeUseCase verificationChallengeUseCase;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort;

    @Override
    @Transactional
    public Result resetPassword(Command command) {
        String rawToken = command.rawToken();
        String newPassword = command.newPassword();
        LocalDateTime now = LocalDateTime.now();

        // 1) verify token via verification-challenge
        var verify = verificationChallengeUseCase.verifyLink(
                new VerificationChallengeUseCase.VerifyLinkCommand(
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
        verificationChallengeUseCase.consume(new VerificationChallengeUseCase.ConsumeCommand(verify.challengeId()));

        // 5) revoke sessions (optional but recommended)
        authSessionRepositoryPort.revokeAllByUserId(user.getId(), now);

        log.info("[PasswordReset] Password reset for userId={} email={}", user.getId(), user.getEmail());
        return new Result(true);
    }
}