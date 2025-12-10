package com.timeeconomy.auth_service.domain.port.out;

import com.timeeconomy.auth_service.domain.model.AuthUser;

import java.util.Optional;

public interface AuthUserRepositoryPort {

    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findById(Long id);

    Optional<AuthUser> findByPhoneNumber(String phoneNumber);

    AuthUser save(AuthUser user);
}