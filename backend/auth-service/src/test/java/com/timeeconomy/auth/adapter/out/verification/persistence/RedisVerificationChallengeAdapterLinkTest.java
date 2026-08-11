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
class RedisVerificationChallengeAdapterLinkTest {

    private static final String CHALLENGE_ID = "30000000-0000-0000-0000-000000000003";
    private static final String RAW_LINK_KEY = "vc:raw:link:" + CHALLENGE_ID;
    private static final String CANARY_TOKEN = "raw-link-token-canary";

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
    void peekReadsWithoutDeletingTheRawLinkToken() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RAW_LINK_KEY)).thenReturn(CANARY_TOKEN);

        Optional<String> result = adapter.peekLinkToken(CHALLENGE_ID);

        assertEquals(Optional.of(CANARY_TOKEN), result);
        verify(valueOperations).get(RAW_LINK_KEY);
        verify(valueOperations, never()).getAndDelete(anyString());
        verify(redis, never()).delete(anyString());
    }

    @Test
    void deleteUsesAnIdempotentSingleKeyRedisDelete() {
        adapter.deleteLinkToken(CHALLENGE_ID);

        verify(redis).delete(RAW_LINK_KEY);
    }
}
