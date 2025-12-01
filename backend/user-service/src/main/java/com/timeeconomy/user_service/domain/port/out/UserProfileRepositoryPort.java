package com.timeeconomy.user_service.domain.port.out;

import com.timeeconomy.user_service.domain.model.UserProfile;

import java.util.Optional;

public interface UserProfileRepositoryPort {

    Optional<UserProfile> findById(Long id);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    UserProfile save(UserProfile userProfile);
}