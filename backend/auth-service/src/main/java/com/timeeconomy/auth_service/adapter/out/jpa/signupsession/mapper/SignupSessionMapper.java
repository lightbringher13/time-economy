package com.timeeconomy.auth_service.adapter.out.jpa.signupsession.mapper;

import com.timeeconomy.auth_service.adapter.out.jpa.signupsession.entity.SignupSessionEntity;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;

import org.springframework.stereotype.Component;

@Component
public class SignupSessionMapper {

    public SignupSessionEntity toEntity(SignupSession d) {
        if (d == null) return null;

        SignupSessionEntity e = new SignupSessionEntity();
        e.setId(d.getId());
        e.setEmail(d.getEmail());
        e.setEmailVerified(d.isEmailVerified());
        e.setPhoneNumber(d.getPhoneNumber());
        e.setPhoneVerified(d.isPhoneVerified());
        e.setName(d.getName());
        e.setGender(d.getGender());
        e.setBirthDate(d.getBirthDate());
        e.setState(d.getState());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setExpiresAt(d.getExpiresAt());
        return e;
    }

    public SignupSession toDomain(SignupSessionEntity e) {
        if (e == null) return null;

        SignupSession d = new SignupSession();
        d.setId(e.getId());
        d.setEmail(e.getEmail());
        d.setEmailVerified(e.isEmailVerified());
        d.setPhoneNumber(e.getPhoneNumber());
        d.setPhoneVerified(e.isPhoneVerified());
        d.setName(e.getName());
        d.setGender(e.getGender());
        d.setBirthDate(e.getBirthDate());
        d.setState(e.getState());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setExpiresAt(e.getExpiresAt());
        return d;
    }
}