package com.timeeconomy.auth_service.adapter.out.jpa.repository;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, Long> {

    Optional<AuthUserEntity> findByEmail(String email);
}