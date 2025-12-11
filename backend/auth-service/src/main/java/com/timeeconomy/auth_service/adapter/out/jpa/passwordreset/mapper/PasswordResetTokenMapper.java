package com.timeeconomy.auth_service.adapter.out.jpa.passwordreset.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.passwordreset.entity.PasswordResetTokenEntity;
import com.timeeconomy.auth_service.domain.passwordreset.model.PasswordResetToken;

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