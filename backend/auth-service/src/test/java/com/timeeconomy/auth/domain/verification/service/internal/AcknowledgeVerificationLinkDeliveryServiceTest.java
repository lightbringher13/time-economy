package com.timeeconomy.auth.domain.verification.service.internal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationLinkDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AcknowledgeVerificationLinkDeliveryServiceTest {

    private static final String CHALLENGE_ID = "30000000-0000-0000-0000-000000000003";

    @Mock
    private VerificationChallengeRepositoryPort repo;

    @InjectMocks
    private AcknowledgeVerificationLinkDeliveryService service;

    @Test
    void deletesTheRawLinkTokenForAnAcknowledgedDelivery() {
        service.acknowledge(new AcknowledgeVerificationLinkDeliveryUseCase.Command(CHALLENGE_ID));

        verify(repo).deleteLinkToken(CHALLENGE_ID);
    }

    @Test
    void ignoresBlankCommandsToKeepAcknowledgmentIdempotent() {
        service.acknowledge(new AcknowledgeVerificationLinkDeliveryUseCase.Command(" "));
        service.acknowledge(null);

        verify(repo, never()).deleteLinkToken(CHALLENGE_ID);
    }
}
