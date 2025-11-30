package com.timeeconomy.auth_service.adapter.out.jpa.repository;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findByTokenHash(String tokenHash);

    List<AuthSessionEntity> findByUserIdAndRevokedFalse(Long userId);
}