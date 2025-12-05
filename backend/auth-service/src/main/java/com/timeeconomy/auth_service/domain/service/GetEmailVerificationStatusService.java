package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.port.in.GetEmailVerificationStatusUseCase;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetEmailVerificationStatusService implements GetEmailVerificationStatusUseCase {

    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public StatusResult getStatus(String email) {
        return emailVerificationRepositoryPort.findLatestByEmail(email)
                .map(verification -> new StatusResult(verification.isVerified()))
                .orElseGet(() -> new StatusResult(false));
    }
}