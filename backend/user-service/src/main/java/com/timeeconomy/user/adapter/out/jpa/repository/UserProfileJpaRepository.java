package com.timeeconomy.user.adapter.out.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timeeconomy.user.adapter.out.jpa.entity.UserProfileEntity;

import java.util.Optional;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}