package com.timeeconomy.auth.domain.verification.service.internal;

import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationOtpDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcknowledgeVerificationOtpDeliveryService implements AcknowledgeVerificationOtpDeliveryUseCase {

    private final VerificationChallengeRepositoryPort repo;

    @Override
    public void acknowledge(Command command) {
        if (command == null || command.challengeId() == null || command.challengeId().isBlank()) {
            return;
        }
        repo.deleteOtp(command.challengeId());
    }
}
