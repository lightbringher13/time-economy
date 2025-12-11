package com.timeeconomy.auth_service.adapter.out.jpa.auth.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.auth.entity.AuthSessionEntity;
import com.timeeconomy.auth_service.domain.auth.model.AuthSession;

import org.springframework.stereotype.Component;

@Component
public class AuthSessionMapper {

    public AuthSessionEntity toEntity(AuthSession domain) {
        if (domain == null) return null;

        AuthSessionEntity e = new AuthSessionEntity();
        e.setId(domain.getId());
        e.setUserId(domain.getUserId());
        e.setFamilyId(domain.getFamilyId());
        e.setTokenHash(domain.getTokenHash());
        e.setDeviceInfo(domain.getDeviceInfo());
        e.setIpAddress(domain.getIpAddress());
        e.setUserAgent(domain.getUserAgent());
        e.setCreatedAt(domain.getCreatedAt());
        e.setLastUsedAt(domain.getLastUsedAt());
        e.setExpiresAt(domain.getExpiresAt());
        e.setRevoked(domain.isRevoked());
        e.setRevokedAt(domain.getRevokedAt());
        e.setReuseDetected(domain.isReuseDetected());
        return e;
    }

    public AuthSession toDomain(AuthSessionEntity e) {
        if (e == null) return null;

        AuthSession d = new AuthSession();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setFamilyId(e.getFamilyId());
        d.setTokenHash(e.getTokenHash());
        d.setDeviceInfo(e.getDeviceInfo());
        d.setIpAddress(e.getIpAddress());
        d.setUserAgent(e.getUserAgent());
        d.setCreatedAt(e.getCreatedAt());
        d.setLastUsedAt(e.getLastUsedAt());
        d.setExpiresAt(e.getExpiresAt());
        d.setRevoked(e.isRevoked());
        d.setRevokedAt(e.getRevokedAt());
        d.setReuseDetected(e.isReuseDetected());
        return d;
    }
}