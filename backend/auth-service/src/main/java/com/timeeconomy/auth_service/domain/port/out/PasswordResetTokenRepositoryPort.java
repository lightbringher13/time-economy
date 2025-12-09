package com.timeeconomy.auth_service.domain.port.out;

import com.timeeconomy.auth_service.domain.model.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findValidByTokenHash(String tokenHash, LocalDateTime now);
}