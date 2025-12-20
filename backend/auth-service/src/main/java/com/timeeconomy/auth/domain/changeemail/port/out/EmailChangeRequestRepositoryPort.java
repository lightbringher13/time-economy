// src/main/java/com/timeeconomy/auth_service/domain/port/out/EmailChangeRequestRepositoryPort.java
package com.timeeconomy.auth.domain.changeemail.port.out;

import java.util.Optional;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public interface EmailChangeRequestRepositoryPort {

    EmailChangeRequest save(EmailChangeRequest request);

    Optional<EmailChangeRequest> findActiveByUserId(Long userId);

    Optional<EmailChangeRequest> findByIdAndUserId(Long id, Long userId);

    void delete(EmailChangeRequest request);

    // optional: for cleanup, queries, etc.
    Optional<EmailChangeRequest> findByUserIdAndStatus(Long userId, EmailChangeStatus status);

    Optional<EmailChangeRequest> findById(Long requestId);
}