package com.timeeconomy.auth_service.adapter.out.jpa.signupsession;

import com.timeeconomy.auth_service.adapter.out.jpa.signupsession.entity.SignupSessionEntity;
import com.timeeconomy.auth_service.adapter.out.jpa.signupsession.mapper.SignupSessionMapper;
import com.timeeconomy.auth_service.adapter.out.jpa.signupsession.repository.SignupSessionJpaRepository;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth_service.domain.signupsession.port.out.SignupSessionRepositoryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SignupSessionJpaAdapter implements SignupSessionRepositoryPort {

    private final SignupSessionJpaRepository jpaRepository;
    private final SignupSessionMapper mapper;

    @Override
    public SignupSession save(SignupSession session) {
        SignupSessionEntity entity = mapper.toEntity(session);
        SignupSessionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SignupSession> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    // ⭐ NEW
    @Override
    public Optional<SignupSession> findLatestActiveByEmail(String email, LocalDateTime now) {
        return jpaRepository
                .findTopByEmailAndExpiresAtAfterAndStateNotOrderByCreatedAtDesc(
                        email,
                        now,
                        SignupSessionState.COMPLETED
                )
                .map(mapper::toDomain);
    }
}