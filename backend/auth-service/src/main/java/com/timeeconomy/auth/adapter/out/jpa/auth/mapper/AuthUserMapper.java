package com.timeeconomy.auth.adapter.out.jpa.auth.mapper;

import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.jpa.auth.entity.AuthUserEntity;
import com.timeeconomy.auth.domain.auth.model.AuthStatus;
import com.timeeconomy.auth.domain.auth.model.AuthUser;

@Component
public class AuthUserMapper {

    // ---------------------------------------------------------
    // Domain → Entity
    // ---------------------------------------------------------
    public AuthUserEntity toEntity(AuthUser domain) {
        if (domain == null) return null;

        AuthUserEntity e = new AuthUserEntity();
        e.setId(domain.getId());
        e.setEmail(domain.getEmail());
        e.setPasswordHash(domain.getPasswordHash());
        e.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);

        e.setFailedLoginAttempts(domain.getFailedLoginAttempts());
        e.setLockedAt(domain.getLockedAt());       // ✅ Instant
        e.setLastLoginAt(domain.getLastLoginAt()); // ✅ Instant

        e.setPhoneNumber(domain.getPhoneNumber());
        e.setEmailVerified(domain.isEmailVerified());
        e.setPhoneVerified(domain.isPhoneVerified());

        e.setCreatedAt(domain.getCreatedAt()); // ✅ Instant
        e.setUpdatedAt(domain.getUpdatedAt()); // ✅ Instant

        return e;
    }

    // ---------------------------------------------------------
    // Entity → Domain
    // ---------------------------------------------------------
    public AuthUser toDomain(AuthUserEntity e) {
        if (e == null) return null;

        AuthUser d = new AuthUser();
        d.setId(e.getId());
        d.setEmail(e.getEmail());
        d.setPasswordHash(e.getPasswordHash());
        d.setStatus(e.getStatus() != null ? AuthStatus.valueOf(e.getStatus()) : null);

        d.setFailedLoginAttempts(e.getFailedLoginAttempts());
        d.setLockedAt(e.getLockedAt());       // ✅ Instant
        d.setLastLoginAt(e.getLastLoginAt()); // ✅ Instant

        d.setPhoneNumber(e.getPhoneNumber());
        d.setEmailVerified(e.isEmailVerified());
        d.setPhoneVerified(e.isPhoneVerified());

        d.setCreatedAt(e.getCreatedAt()); // ✅ Instant
        d.setUpdatedAt(e.getUpdatedAt()); // ✅ Instant

        return d;
    }
}