package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.adapter.out.jpa.repository.AuthUserJpaRepository;
import com.timeeconomy.auth_service.adapter.out.jpa.entity.AuthUserEntity;
import com.timeeconomy.auth_service.adapter.out.jpa.mapper.AuthUserMapper;
import com.timeeconomy.auth_service.domain.model.AuthUser;
import com.timeeconomy.auth_service.domain.port.out.AuthUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUserJpaAdapter implements AuthUserRepositoryPort {

    private final AuthUserJpaRepository jpaRepository;
    private final AuthUserMapper mapper;

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AuthUser> findByPhoneNumber(String phoneNumber) {
        return jpaRepository.findByPhoneNumber(phoneNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AuthUser> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public AuthUser save(AuthUser user) {
        AuthUserEntity entity = mapper.toEntity(user);
        AuthUserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}