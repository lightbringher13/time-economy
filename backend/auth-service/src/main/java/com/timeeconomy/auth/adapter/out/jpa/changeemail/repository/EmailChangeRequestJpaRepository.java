// src/main/java/com/timeeconomy/auth_service/adapter/out/persistence/EmailChangeRequestJpaRepository.java
package com.timeeconomy.auth.adapter.out.jpa.changeemail.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timeeconomy.auth.adapter.out.jpa.changeemail.entity.EmailChangeRequestEntity;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

import java.util.Optional;

public interface EmailChangeRequestJpaRepository
        extends JpaRepository<EmailChangeRequestEntity, Long> {

    Optional<EmailChangeRequestEntity> findFirstByUserIdAndStatusIn(
            Long userId,
            Iterable<EmailChangeStatus> statuses
    );

    Optional<EmailChangeRequestEntity> findByIdAndUserId(Long id, Long userId);

    Optional<EmailChangeRequestEntity> findByUserIdAndStatus(Long userId, EmailChangeStatus status);
}