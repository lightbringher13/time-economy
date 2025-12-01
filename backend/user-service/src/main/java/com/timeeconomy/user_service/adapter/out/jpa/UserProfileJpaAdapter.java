package com.timeeconomy.user_service.adapter.out.jpa;

import com.timeeconomy.user_service.adapter.out.jpa.entity.UserProfileEntity;
import com.timeeconomy.user_service.adapter.out.jpa.mapper.UserProfileMapper;
import com.timeeconomy.user_service.adapter.out.jpa.repository.UserProfileJpaRepository;
import com.timeeconomy.user_service.domain.model.UserProfile;
import com.timeeconomy.user_service.domain.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserProfileJpaAdapter implements UserProfileRepositoryPort {

    private final UserProfileJpaRepository jpaRepository;
    private final UserProfileMapper mapper;

    @Override
    public Optional<UserProfile> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileEntity entity = mapper.toEntity(userProfile);
        UserProfileEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}