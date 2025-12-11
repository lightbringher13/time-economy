package com.timeeconomy.auth_service.adapter.out.jpa.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timeeconomy.auth_service.adapter.out.jpa.auth.entity.AuthUserEntity;

import java.util.Optional;

public interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, Long> {

    Optional<AuthUserEntity> findByEmail(String email);

    Optional<AuthUserEntity> findByPhoneNumber(String phoneNumber);
}