package com.timeeconomy.auth_service.adapter.out.jpa.repository;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.PhoneVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhoneVerificationJpaRepository extends JpaRepository<PhoneVerificationEntity, Long> {

    Optional<PhoneVerificationEntity> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    Optional<PhoneVerificationEntity> findByPhoneNumberAndCode(String phoneNumber, String code);
}