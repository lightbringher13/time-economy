package com.timeeconomy.auth.domain.verification.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

@ExtendWith(MockitoExtension.class)
class PeekVerificationOtpServiceTest {

    private static final String CHALLENGE_ID = "20000000-0000-0000-0000-000000000002";
    private static final String CANARY_OTP = "731942";
    private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");

    @Mock
    private VerificationChallengeRepositoryPort repo;

    private PeekVerificationOtpService service;

    @BeforeEach
    void setUp() {
        service = new PeekVerificationOtpService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void peeksPendingUnexpiredOtpWithoutDeletingIt() {
        VerificationChallenge challenge = otpChallenge(NOW.plusSeconds(300));
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(repo.peekOtp(CHALLENGE_ID)).thenReturn(Optional.of(CANARY_OTP));

        PeekVerificationOtpUseCase.Result result = service.peek(
                new PeekVerificationOtpUseCase.Command(CHALLENGE_ID)
        );

        assertEquals(CHALLENGE_ID, result.verificationChallengeId());
        assertEquals(CANARY_OTP, result.otp());
        assertFalse(result.toString().contains(CANARY_OTP));
        verify(repo).peekOtp(CHALLENGE_ID);
        verify(repo, never()).deleteOtp(CHALLENGE_ID);
    }

    @Test
    void rejectsCanceledOtpBeforeReadingTheRawCode() {
        VerificationChallenge challenge = otpChallenge(NOW.plusSeconds(300));
        challenge.cancel(NOW);
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThrows(
                NoSuchElementException.class,
                () -> service.peek(new PeekVerificationOtpUseCase.Command(CHALLENGE_ID))
        );

        verify(repo, never()).peekOtp(CHALLENGE_ID);
    }

    @Test
    void rejectsExpiredOtpBeforeReadingTheRawCode() {
        VerificationChallenge challenge = otpChallenge(NOW.minusSeconds(1));
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThrows(
                NoSuchElementException.class,
                () -> service.peek(new PeekVerificationOtpUseCase.Command(CHALLENGE_ID))
        );

        verify(repo, never()).peekOtp(CHALLENGE_ID);
    }

    @Test
    void rejectsLinkChallengeBeforeReadingTheRawCode() {
        VerificationChallenge challenge = VerificationChallenge.createLinkPending(
                VerificationPurpose.SIGNUP_EMAIL,
                VerificationChannel.EMAIL,
                VerificationSubjectType.SIGNUP_SESSION,
                "signup-session-id",
                "member@example.test",
                "member@example.test",
                "link-token-hash",
                NOW.plusSeconds(300),
                5,
                NOW,
                "127.0.0.1",
                "test-agent"
        );
        challenge.setId(CHALLENGE_ID);
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThrows(
                NoSuchElementException.class,
                () -> service.peek(new PeekVerificationOtpUseCase.Command(CHALLENGE_ID))
        );

        verify(repo, never()).peekOtp(CHALLENGE_ID);
    }

    @Test
    void reportsNotFoundWhenRawOtpIsMissing() {
        VerificationChallenge challenge = otpChallenge(NOW.plusSeconds(300));
        when(repo.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(repo.peekOtp(CHALLENGE_ID)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.peek(new PeekVerificationOtpUseCase.Command(CHALLENGE_ID))
        );
    }

    private VerificationChallenge otpChallenge(Instant expiresAt) {
        VerificationChallenge challenge = VerificationChallenge.createOtpPending(
                VerificationPurpose.SIGNUP_PHONE,
                VerificationChannel.SMS,
                VerificationSubjectType.SIGNUP_SESSION,
                "signup-session-id",
                "+821012345678",
                "+821012345678",
                "otp-code-hash",
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
