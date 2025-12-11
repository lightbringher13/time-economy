package com.timeeconomy.auth_service.domain.emailverification.port.out;

import java.util.Optional;

import com.timeeconomy.auth_service.domain.emailverification.model.EmailVerification;

public interface EmailVerificationRepositoryPort {

    EmailVerification save(EmailVerification verification);

    Optional<EmailVerification> findLatestByEmail(String email);

    Optional<EmailVerification> findByEmailAndCode(String email, String code);

}