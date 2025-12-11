package com.timeeconomy.auth_service.adapter.out.jpa.emailverification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timeeconomy.auth_service.adapter.out.jpa.emailverification.entity.EmailVerificationEntity;

import java.util.Optional;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationEntity, Long> {

    Optional<EmailVerificationEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailVerificationEntity> findByEmailAndCode(String email, String code);
}