package com.timeeconomy.auth.adapter.out.jpa.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.adapter.out.jpa.auth.entity.AuthSessionEntity;
import com.timeeconomy.auth.adapter.out.jpa.auth.mapper.AuthSessionMapper;
import com.timeeconomy.auth.adapter.out.jpa.auth.repository.AuthSessionJpaRepository;
import com.timeeconomy.auth.domain.auth.model.AuthSession;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;

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

    // ✅ 새로 추가한 메서드 구현
    @Override
    public Optional<AuthSession> findLatestActiveByFamily(String familyId, LocalDateTime now) {
        return jpaRepository.findLatestActiveByFamily(familyId, now)
                .map(mapper::toDomain);
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
    public Optional<AuthSession> findByTokenHashForUpdate(String tokenHash) {
        AuthSessionEntity e = jpaRepository.findByTokenHashForUpdate(tokenHash);
        return Optional.ofNullable(mapper.toDomain(e));
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
    @Transactional
    public void revokeAllByUserId(Long userId, LocalDateTime now) {
        List<AuthSessionEntity> sessions = jpaRepository.findByUserIdAndRevokedFalse(userId);

        if (sessions.isEmpty()) {
            return;
        }

        for (AuthSessionEntity entity : sessions) {
            entity.setRevoked(true);
            entity.setRevokedAt(now);
        }

        jpaRepository.saveAll(sessions);
    }

    @Override
    @Transactional
    public void revokeFamily(String familyId, LocalDateTime now) {
        jpaRepository.revokeFamily(familyId, now);
    }
}