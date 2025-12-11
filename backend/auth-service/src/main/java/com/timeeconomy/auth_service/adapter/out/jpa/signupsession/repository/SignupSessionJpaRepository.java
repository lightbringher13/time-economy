package com.timeeconomy.auth_service.adapter.out.jpa.signupsession.repository;

import com.timeeconomy.auth_service.adapter.out.jpa.signupsession.entity.SignupSessionEntity;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSessionState;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

public interface SignupSessionJpaRepository extends JpaRepository<SignupSessionEntity, UUID> {
    // For now we only need the basic CRUD by ID.
    // Later you can add:
    // Optional<SignupSessionEntity> findFirstByEmailAndStateInOrderByCreatedAtDesc(...)

    Optional<SignupSessionEntity> findTopByEmailAndExpiresAtAfterAndStateNotOrderByCreatedAtDesc(
    String email,
    LocalDateTime expiresAt,
    SignupSessionState excludedState
);
}