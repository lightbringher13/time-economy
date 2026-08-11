package com.timeeconomy.auth.adapter.out.verification.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisVerificationChallengeAdapterOtpTest {

    private static final String CHALLENGE_ID = "20000000-0000-0000-0000-000000000002";
    private static final String RAW_OTP_KEY = "vc:raw:otp:" + CHALLENGE_ID;
    private static final String CANARY_OTP = "731942";

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisVerificationChallengeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisVerificationChallengeAdapter(redis, Clock.systemUTC());
    }

    @Test
    void peekReadsWithoutDeletingTheRawOtp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RAW_OTP_KEY)).thenReturn(CANARY_OTP);

        Optional<String> result = adapter.peekOtp(CHALLENGE_ID);

        assertEquals(Optional.of(CANARY_OTP), result);
        verify(valueOperations).get(RAW_OTP_KEY);
        verify(valueOperations, never()).getAndDelete(anyString());
        verify(redis, never()).delete(anyString());
    }

    @Test
    void deleteUsesAnIdempotentSingleKeyRedisDelete() {
        adapter.deleteOtp(CHALLENGE_ID);

        verify(redis).delete(RAW_OTP_KEY);
    }
}
