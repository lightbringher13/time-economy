package com.timeeconomy.auth_service.domain.port.out;

import com.timeeconomy.auth_service.domain.model.EmailVerification;

import java.util.Optional;

public interface EmailVerificationRepositoryPort {

    EmailVerification save(EmailVerification verification);

    Optional<EmailVerification> findLatestByEmail(String email);

    Optional<EmailVerification> findByEmailAndCode(String email, String code);

}