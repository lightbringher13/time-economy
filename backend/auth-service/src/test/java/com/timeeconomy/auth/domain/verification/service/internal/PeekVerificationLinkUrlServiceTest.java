package com.timeeconomy.auth.domain.verification.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationLinkUrlUseCase;
import com.timeeconomy.auth.domain.verification.port.out.internal.VerificationLinkBuilderPort;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

@ExtendWith(MockitoExtension.class)
class PeekVerificationLinkUrlServiceTest {

    private static final String CHALLENGE_ID = "30000000-0000-0000-0000-000000000003";
    private static final String CANARY_TOKEN = "raw-link-token-canary";
    private static final String CANARY_URL = "https://app.example.test/reset-password?token=" + CANARY_TOKEN;
    private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");

    @Mock
    private VerificationChallengeRepositoryPort repo;
    @Mock
    private VerificationLinkBuilderPort linkBuilder;

    private PeekVerificationLinkUrlService service;

    @BeforeEach
    void setUp() {
        service = new PeekVerificationLinkUrlService(
                repo,
                linkBuilder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void repeatedlyPeeksPendingUnexpiredEmailLinkWithoutDeletingIt() {
        VerificationChallenge challenge = linkChallenge(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                NOW.plusSeconds(300)
        );
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(repo.peekLinkToken(CHALLENGE_ID)).thenReturn(Optional.of(CANARY_TOKEN));
        when(linkBuilder.buildLinkUrl(CANARY_TOKEN, VerificationPurpose.PASSWORD_RESET))
                .thenReturn(CANARY_URL);

        PeekVerificationLinkUrlUseCase.Command command = new PeekVerificationLinkUrlUseCase.Command(
                CHALLENGE_ID,
                VerificationPurpose.PASSWORD_RESET
        );
        PeekVerificationLinkUrlUseCase.Result first = service.peek(command);
        PeekVerificationLinkUrlUseCase.Result second = service.peek(command);

        assertEquals(CHALLENGE_ID, first.verificationChallengeId());
        assertEquals(CANARY_URL, first.linkUrl());
        assertEquals(CANARY_URL, second.linkUrl());
        assertFalse(first.toString().contains(CANARY_TOKEN));
        assertFalse(first.toString().contains(CANARY_URL));
        verify(repo, times(2)).peekLinkToken(CHALLENGE_ID);
        verify(repo, never()).deleteLinkToken(CHALLENGE_ID);
        verify(linkBuilder, times(2)).buildLinkUrl(CANARY_TOKEN, VerificationPurpose.PASSWORD_RESET);
    }

    @Test
    void rejectsCanceledLinkBeforeReadingTheRawToken() {
        VerificationChallenge challenge = linkChallenge(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                NOW.plusSeconds(300)
        );
        challenge.cancel(NOW);
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertNotFound(VerificationPurpose.PASSWORD_RESET);

        verify(repo, never()).peekLinkToken(CHALLENGE_ID);
        verifyNoInteractions(linkBuilder);
    }

    @Test
    void rejectsExpiredLinkBeforeReadingTheRawToken() {
        VerificationChallenge challenge = linkChallenge(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                NOW.minusSeconds(1)
        );
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertNotFound(VerificationPurpose.PASSWORD_RESET);

        verify(repo, never()).peekLinkToken(CHALLENGE_ID);
        verifyNoInteractions(linkBuilder);
    }

    @Test
    void rejectsOtpChallengeBeforeReadingTheRawToken() {
        VerificationChallenge challenge = VerificationChallenge.createOtpPending(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                VerificationSubjectType.EMAIL,
                "member@example.test",
                "member@example.test",
                "member@example.test",
                "otp-code-hash",
                NOW.plusSeconds(300),
                5,
                NOW,
                "127.0.0.1",
                "test-agent"
        );
        challenge.setId(CHALLENGE_ID);
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertNotFound(VerificationPurpose.PASSWORD_RESET);

        verify(repo, never()).peekLinkToken(CHALLENGE_ID);
        verifyNoInteractions(linkBuilder);
    }

    @Test
    void rejectsPurposeMismatchBeforeReadingTheRawToken() {
        VerificationChallenge challenge = linkChallenge(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                NOW.plusSeconds(300)
        );
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertNotFound(VerificationPurpose.SIGNUP_EMAIL);

        verify(repo, never()).peekLinkToken(CHALLENGE_ID);
        verifyNoInteractions(linkBuilder);
    }

    @Test
    void rejectsNonEmailLinkBeforeReadingTheRawToken() {
        VerificationChallenge challenge = linkChallenge(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.SMS,
                NOW.plusSeconds(300)
        );
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertNotFound(VerificationPurpose.PASSWORD_RESET);

        verify(repo, never()).peekLinkToken(CHALLENGE_ID);
        verifyNoInteractions(linkBuilder);
    }

    @Test
    void reportsNotFoundWhenRawLinkTokenIsMissing() {
        VerificationChallenge challenge = linkChallenge(
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                NOW.plusSeconds(300)
        );
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(repo.peekLinkToken(CHALLENGE_ID)).thenReturn(Optional.empty());

        assertNotFound(VerificationPurpose.PASSWORD_RESET);

        verifyNoInteractions(linkBuilder);
    }

    private void assertNotFound(VerificationPurpose purpose) {
        assertThrows(
                NoSuchElementException.class,
                () -> service.peek(new PeekVerificationLinkUrlUseCase.Command(CHALLENGE_ID, purpose))
        );
    }

    private VerificationChallenge linkChallenge(
            VerificationPurpose purpose,
            VerificationChannel channel,
            Instant expiresAt
    ) {
        VerificationChallenge challenge = VerificationChallenge.createLinkPending(
                purpose,
                channel,
                VerificationSubjectType.EMAIL,
                "member@example.test",
                "member@example.test",
                "member@example.test",
                "link-token-hash",
                expiresAt,
                5,
                NOW,
                "127.0.0.1",
                "test-agent"
        );
        challenge.setId(CHALLENGE_ID);
        return challenge;
    }
}
