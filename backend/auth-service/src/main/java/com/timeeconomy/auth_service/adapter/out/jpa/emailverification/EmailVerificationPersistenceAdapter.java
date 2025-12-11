package com.timeeconomy.auth_service.adapter.out.jpa.emailverification;

import com.timeeconomy.auth_service.adapter.out.jpa.emailverification.entity.EmailVerificationEntity;
import com.timeeconomy.auth_service.adapter.out.jpa.emailverification.mapper.EmailVerificationMapper;
import com.timeeconomy.auth_service.adapter.out.jpa.emailverification.repository.EmailVerificationJpaRepository;
import com.timeeconomy.auth_service.domain.emailverification.model.EmailVerification;
import com.timeeconomy.auth_service.domain.emailverification.port.out.EmailVerificationRepositoryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailVerificationPersistenceAdapter implements EmailVerificationRepositoryPort {

    private final EmailVerificationJpaRepository repo;
    private final EmailVerificationMapper mapper;

    @Override
    public EmailVerification save(EmailVerification verification) {
        EmailVerificationEntity saved = repo.save(mapper.toEntity(verification));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EmailVerification> findLatestByEmail(String email) {
        return repo.findTopByEmailOrderByCreatedAtDesc(email)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<EmailVerification> findByEmailAndCode(String email, String code) {
        return repo.findByEmailAndCode(email, code)
                .map(mapper::toDomain);
    }
}