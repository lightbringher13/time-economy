package com.timeeconomy.auth.adapter.out.jpa.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.jpa.auth.entity.AuthUserEntity;
import com.timeeconomy.auth.adapter.out.jpa.auth.mapper.AuthUserMapper;
import com.timeeconomy.auth.adapter.out.jpa.auth.repository.AuthUserJpaRepository;
import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;

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