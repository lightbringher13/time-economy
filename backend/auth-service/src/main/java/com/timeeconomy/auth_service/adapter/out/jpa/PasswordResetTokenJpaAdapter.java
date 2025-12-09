package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.PasswordResetTokenEntity;
import com.timeeconomy.auth_service.adapter.out.jpa.mapper.PasswordResetTokenMapper;
import com.timeeconomy.auth_service.adapter.out.jpa.repository.SpringDataPasswordResetTokenRepository;
import com.timeeconomy.auth_service.domain.model.PasswordResetToken;
import com.timeeconomy.auth_service.domain.port.out.PasswordResetTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenRepositoryPort {

    private final SpringDataPasswordResetTokenRepository jpaRepository;
    private final PasswordResetTokenMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = mapper.toEntity(token);
        PasswordResetTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PasswordResetToken> findValidByTokenHash(String tokenHash, LocalDateTime now) {
        return jpaRepository
                .findFirstByTokenHashAndExpiresAtAfterAndUsedAtIsNullOrderByCreatedAtDesc(tokenHash, now)
                .map(mapper::toDomain);
    }
}