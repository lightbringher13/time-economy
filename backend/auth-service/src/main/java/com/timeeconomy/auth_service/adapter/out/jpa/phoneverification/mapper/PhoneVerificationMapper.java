package com.timeeconomy.auth_service.adapter.out.jpa.phoneverification.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.phoneverification.entity.PhoneVerificationEntity;
import com.timeeconomy.auth_service.domain.phoneverification.model.PhoneVerification;

import org.springframework.stereotype.Component;

@Component
public class PhoneVerificationMapper {

    public PhoneVerificationEntity toEntity(PhoneVerification domain) {
        if (domain == null)
            return null;

        return PhoneVerificationEntity.builder()
                .id(domain.getId())
                .phoneNumber(domain.getPhoneNumber())
                .countryCode(domain.getCountryCode())
                .code(domain.getCode())
                .expiresAt(domain.getExpiresAt())
                .verifiedAt(domain.getVerifiedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public PhoneVerification toDomain(PhoneVerificationEntity entity) {
        if (entity == null)
            return null;

        return PhoneVerification.builder()
                .id(entity.getId())
                .phoneNumber(entity.getPhoneNumber())
                .countryCode(entity.getCountryCode())
                .code(entity.getCode())
                .expiresAt(entity.getExpiresAt())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}