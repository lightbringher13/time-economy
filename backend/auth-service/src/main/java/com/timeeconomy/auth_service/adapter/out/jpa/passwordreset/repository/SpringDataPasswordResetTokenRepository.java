package com.timeeconomy.auth_service.adapter.out.jpa.passwordreset.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timeeconomy.auth_service.adapter.out.jpa.passwordreset.entity.PasswordResetTokenEntity;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataPasswordResetTokenRepository
        extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity>
    findFirstByTokenHashAndExpiresAtAfterAndUsedAtIsNullOrderByCreatedAtDesc(
            String tokenHash,
            LocalDateTime now
    );
}