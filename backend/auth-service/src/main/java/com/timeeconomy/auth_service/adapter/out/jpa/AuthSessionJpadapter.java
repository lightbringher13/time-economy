package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.AuthSessionEntity;
import com.timeeconomy.auth_service.adapter.out.jpa.mapper.AuthSessionMapper;
import com.timeeconomy.auth_service.adapter.out.jpa.repository.AuthSessionJpaRepository;
import com.timeeconomy.auth_service.domain.model.AuthSession;
import com.timeeconomy.auth_service.domain.port.out.AuthSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthSessionJpadapter implements AuthSessionRepositoryPort {

    private final AuthSessionJpaRepository jpaRepository;
    private final AuthSessionMapper mapper;

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionEntity entity = mapper.toEntity(session);
        AuthSessionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AuthSession> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AuthSession> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(mapper::toDomain);
    }

    @Override
    public List<AuthSession> findActiveByUserId(Long userId) {
        return jpaRepository.findByUserIdAndRevokedFalse(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void revokeById(Long id, LocalDateTime now) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setRevoked(true);
            entity.setRevokedAt(now);
            jpaRepository.save(entity);
        });
    }

    @Override
    public void revokeAllByUserId(Long userId, LocalDateTime now) {
        List<AuthSessionEntity> sessions =
                jpaRepository.findByUserIdAndRevokedFalse(userId);

        if (sessions.isEmpty()) {
            return;
        }

        for (AuthSessionEntity entity : sessions) {
            entity.setRevoked(true);
            entity.setRevokedAt(now);
        }

        jpaRepository.saveAll(sessions);
    }
}