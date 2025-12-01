package com.timeeconomy.user_service.adapter.out.jpa.repository;

import com.timeeconomy.user_service.adapter.out.jpa.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}