package com.timeeconomy.auth.domain.verification.service.internal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationOtpDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AcknowledgeVerificationOtpDeliveryServiceTest {

    private static final String CHALLENGE_ID = "20000000-0000-0000-0000-000000000002";

    @Mock
    private VerificationChallengeRepositoryPort repo;

    @InjectMocks
    private AcknowledgeVerificationOtpDeliveryService service;

    @Test
    void deletesTheRawOtpForAnAcknowledgedDelivery() {
        service.acknowledge(new AcknowledgeVerificationOtpDeliveryUseCase.Command(CHALLENGE_ID));

        verify(repo).deleteOtp(CHALLENGE_ID);
    }

    @Test
    void ignoresBlankCommandsToKeepAcknowledgmentIdempotent() {
        service.acknowledge(new AcknowledgeVerificationOtpDeliveryUseCase.Command(" "));
        service.acknowledge(null);

        verify(repo, never()).deleteOtp(CHALLENGE_ID);
    }
}
