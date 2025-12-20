// src/main/java/com/timeeconomy/auth_service/adapter/out/persistence/EmailChangeRequestJpaAdapter.java
package com.timeeconomy.auth.adapter.out.jpa.changeemail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.jpa.changeemail.entity.EmailChangeRequestEntity;
import com.timeeconomy.auth.adapter.out.jpa.changeemail.mapper.EmailChangeRequestMapper;
import com.timeeconomy.auth.adapter.out.jpa.changeemail.repository.EmailChangeRequestJpaRepository;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;

import java.util.EnumSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailChangeRequestJpaAdapter implements EmailChangeRequestRepositoryPort {

    private final EmailChangeRequestJpaRepository repository;

    @Override
    public EmailChangeRequest save(EmailChangeRequest request) {
        EmailChangeRequestEntity entity = EmailChangeRequestMapper.toEntity(request);
        EmailChangeRequestEntity saved = repository.save(entity);
        return EmailChangeRequestMapper.toDomain(saved);
    }

    @Override
    public Optional<EmailChangeRequest> findActiveByUserId(Long userId) {
        EnumSet<EmailChangeStatus> activeStatuses = EnumSet.of(
                EmailChangeStatus.PENDING,
                EmailChangeStatus.NEW_EMAIL_VERIFIED,
                EmailChangeStatus.READY_TO_COMMIT
        );
        return repository
                .findFirstByUserIdAndStatusIn(userId, activeStatuses)
                .map(EmailChangeRequestMapper::toDomain);
    }

    @Override
    public Optional<EmailChangeRequest> findByIdAndUserId(Long id, Long userId) {
        return repository
                .findByIdAndUserId(id, userId)
                .map(EmailChangeRequestMapper::toDomain);
    }

    @Override
    public void delete(EmailChangeRequest request) {
        if (request.getId() != null) {
            repository.deleteById(request.getId());
        }
    }

    @Override
    public Optional<EmailChangeRequest> findByUserIdAndStatus(Long userId, EmailChangeStatus status) {
        return repository
                .findByUserIdAndStatus(userId, status)
                .map(EmailChangeRequestMapper::toDomain);
    }

    @Override
    public Optional<EmailChangeRequest> findById(Long requestId) {
        return repository.findById(requestId).map(EmailChangeRequestMapper::toDomain);
    }
}