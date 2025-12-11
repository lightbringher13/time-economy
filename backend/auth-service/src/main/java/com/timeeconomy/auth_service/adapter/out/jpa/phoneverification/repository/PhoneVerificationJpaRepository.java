package com.timeeconomy.auth_service.adapter.out.jpa.phoneverification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timeeconomy.auth_service.adapter.out.jpa.phoneverification.entity.PhoneVerificationEntity;

import java.util.Optional;

public interface PhoneVerificationJpaRepository extends JpaRepository<PhoneVerificationEntity, Long> {

    Optional<PhoneVerificationEntity> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    Optional<PhoneVerificationEntity> findByPhoneNumberAndCode(String phoneNumber, String code);
}