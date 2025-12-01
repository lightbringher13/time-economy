package com.timeeconomy.auth_service.adapter.out.jpa.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.AuthUserEntity;
import com.timeeconomy.auth_service.domain.model.AuthStatus;
import com.timeeconomy.auth_service.domain.model.AuthUser;
import org.springframework.stereotype.Component;

@Component
public class AuthUserMapper {

    public AuthUserEntity toEntity(AuthUser domain) {
        if (domain == null) return null;

        AuthUserEntity e = new AuthUserEntity();
        e.setId(domain.getId());
        e.setUserId(domain.getUserId());
        e.setEmail(domain.getEmail());
        e.setPasswordHash(domain.getPasswordHash());
        e.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        e.setFailedLoginAttempts(domain.getFailedLoginAttempts());
        e.setLockedAt(domain.getLockedAt());
        e.setLastLoginAt(domain.getLastLoginAt());
        e.setCreatedAt(domain.getCreatedAt());
        e.setUpdatedAt(domain.getUpdatedAt());
        return e;
    }

    public AuthUser toDomain(AuthUserEntity e) {
        if (e == null) return null;

        AuthUser d = new AuthUser();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setEmail(e.getEmail());
        d.setPasswordHash(e.getPasswordHash());
        d.setStatus(e.getStatus() != null ? AuthStatus.valueOf(e.getStatus()) : null);
        d.setFailedLoginAttempts(e.getFailedLoginAttempts());
        d.setLockedAt(e.getLockedAt());
        d.setLastLoginAt(e.getLastLoginAt());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}