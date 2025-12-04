package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.domain.model.PhoneVerification;
import com.timeeconomy.auth_service.domain.port.out.PhoneVerificationRepositoryPort;
import com.timeeconomy.auth_service.adapter.out.jpa.repository.PhoneVerificationJpaRepository;
import com.timeeconomy.auth_service.adapter.out.jpa.entity.PhoneVerificationEntity;
import com.timeeconomy.auth_service.adapter.out.jpa.mapper.PhoneVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PhoneVerificationJpaAdapter implements PhoneVerificationRepositoryPort {

    private final PhoneVerificationJpaRepository jpaRepository;
    private final PhoneVerificationMapper mapper;

    @Override
    public PhoneVerification save(PhoneVerification verification) {
        PhoneVerificationEntity saved = jpaRepository.save(
                mapper.toEntity(verification));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PhoneVerification> findLatestByPhoneNumber(String phoneNumber) {
        return jpaRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PhoneVerification> findByPhoneAndCode(String phoneNumber, String code) {
        return jpaRepository.findByPhoneNumberAndCode(phoneNumber, code)
                .map(mapper::toDomain);
    }
}