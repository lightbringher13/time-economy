package com.timeeconomy.user.domain.port.out;

import java.util.Optional;

import com.timeeconomy.user.domain.model.UserProfile;

public interface UserProfileRepositoryPort {

    Optional<UserProfile> findById(Long id);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    UserProfile save(UserProfile userProfile);
}