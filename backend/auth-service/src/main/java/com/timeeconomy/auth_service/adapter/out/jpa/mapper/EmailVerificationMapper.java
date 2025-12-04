package com.timeeconomy.auth_service.adapter.out.jpa.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.EmailVerificationEntity;
import com.timeeconomy.auth_service.domain.model.EmailVerification;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationMapper {

    public EmailVerificationEntity toEntity(EmailVerification domain) {
        return new EmailVerificationEntity(
                domain.getId(),
                domain.getEmail(),
                domain.getCode(),
                domain.getExpiresAt(),
                domain.getVerifiedAt(),
                domain.getCreatedAt());
    }

    public EmailVerification toDomain(EmailVerificationEntity e) {
        return new EmailVerification(
                e.getId(),
                e.getEmail(),
                e.getCode(),
                e.getExpiresAt(),
                e.getVerifiedAt(),
                e.getCreatedAt());
    }
}