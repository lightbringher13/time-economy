package com.timeeconomy.auth.domain.auth.port.out;

import java.util.Optional;

import com.timeeconomy.auth.domain.auth.model.AuthUser;

public interface AuthUserRepositoryPort {

    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findById(Long id);

    Optional<AuthUser> findByPhoneNumber(String phoneNumber);

    AuthUser save(AuthUser user);
}