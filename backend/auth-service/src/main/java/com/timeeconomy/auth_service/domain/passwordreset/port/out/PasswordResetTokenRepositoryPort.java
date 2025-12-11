package com.timeeconomy.auth_service.domain.passwordreset.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

import com.timeeconomy.auth_service.domain.passwordreset.model.PasswordResetToken;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findValidByTokenHash(String tokenHash, LocalDateTime now);
}