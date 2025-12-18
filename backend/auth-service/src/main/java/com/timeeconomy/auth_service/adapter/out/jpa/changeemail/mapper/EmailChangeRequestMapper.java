// src/main/java/com/timeeconomy/auth_service/adapter/out/persistence/mapper/EmailChangeRequestMapper.java
package com.timeeconomy.auth_service.adapter.out.jpa.changeemail.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.changeemail.entity.EmailChangeRequestEntity;
import com.timeeconomy.auth_service.domain.changeemail.model.EmailChangeRequest;

public final class EmailChangeRequestMapper {

    private EmailChangeRequestMapper() {
    }

    public static EmailChangeRequestEntity toEntity(EmailChangeRequest domain) {
        if (domain == null) return null;

        return EmailChangeRequestEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .oldEmail(domain.getOldEmail())
                .newEmail(domain.getNewEmail())
                .secondFactorType(domain.getSecondFactorType())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    public static EmailChangeRequest toDomain(EmailChangeRequestEntity entity) {
        if (entity == null) return null;

        return EmailChangeRequest.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .oldEmail(entity.getOldEmail())
                .newEmail(entity.getNewEmail())
                .secondFactorType(entity.getSecondFactorType())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}