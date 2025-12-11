// src/main/java/com/timeeconomy/auth_service/adapter/out/persistence/EmailChangeRequestJpaRepository.java
package com.timeeconomy.auth_service.adapter.out.jpa.changeemail.repository;

import com.timeeconomy.auth_service.adapter.out.jpa.changeemail.entity.EmailChangeRequestEntity;
import com.timeeconomy.auth_service.domain.changeemail.model.EmailChangeStatus;

import org.springframework.data.jpa.repository.JpaRepository;

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