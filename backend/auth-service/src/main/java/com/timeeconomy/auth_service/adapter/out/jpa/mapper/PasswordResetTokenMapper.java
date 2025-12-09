package com.timeeconomy.auth_service.adapter.out.jpa.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.PasswordResetTokenEntity;
import com.timeeconomy.auth_service.domain.model.PasswordResetToken;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenMapper {

    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return new PasswordResetToken(
                entity.getId(),
                entity.getEmail(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }

    public PasswordResetTokenEntity toEntity(PasswordResetToken domain) {
        if (domain == null) {
            return null;
        }

        return new PasswordResetTokenEntity(
                domain.getId(),
                domain.getEmail(),
                domain.getTokenHash(),
                domain.getExpiresAt(),
                domain.getUsedAt(),
                domain.getCreatedAt()
        );
    }
}