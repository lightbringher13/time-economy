// src/main/java/com/timeeconomy/auth_service/domain/port/out/EmailChangeRequestRepositoryPort.java
package com.timeeconomy.auth_service.domain.port.out;

import com.timeeconomy.auth_service.domain.model.EmailChangeRequest;
import com.timeeconomy.auth_service.domain.model.EmailChangeStatus;

import java.util.Optional;

public interface EmailChangeRequestRepositoryPort {

    EmailChangeRequest save(EmailChangeRequest request);

    Optional<EmailChangeRequest> findActiveByUserId(Long userId);

    Optional<EmailChangeRequest> findByIdAndUserId(Long id, Long userId);

    void delete(EmailChangeRequest request);

    // optional: for cleanup, queries, etc.
    Optional<EmailChangeRequest> findByUserIdAndStatus(Long userId, EmailChangeStatus status);
}