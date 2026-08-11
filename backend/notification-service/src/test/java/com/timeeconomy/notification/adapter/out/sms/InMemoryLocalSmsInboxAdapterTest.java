package com.timeeconomy.notification.adapter.out.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;
import com.timeeconomy.notification.config.LocalSmsInboxProperties;

class InMemoryLocalSmsInboxAdapterTest {

    private static final Instant START = Instant.parse("2026-08-09T12:00:00Z");
    private static final String CANARY_PHONE = "+821012345678";
    private static final String CANARY_OTP = "731942";

    @Test
    void storesNewestFirstAndConsumesEachChallengeOnlyOnce() {
        MutableClock clock = new MutableClock(START);
        InMemoryLocalSmsInboxAdapter inbox = inbox(clock, 10);
        LocalSmsInboxPort.Entry first = entry(1, START.plusSeconds(300), CANARY_OTP);
        LocalSmsInboxPort.Entry second = entry(2, START.plusSeconds(300), "825104");

        inbox.store(first);
        clock.advance(Duration.ofSeconds(1));
        inbox.store(second);

        assertEquals(
                List.of(second.challengeId(), first.challengeId()),
                inbox.findRecent(10).stream().map(LocalSmsInboxPort.Entry::challengeId).toList()
        );
        assertEquals(CANARY_OTP, inbox.consume(first.challengeId()).orElseThrow().otp());
        assertTrue(inbox.consume(first.challengeId()).isEmpty());
        assertFalse(first.toString().contains(CANARY_OTP));
        assertFalse(first.toString().contains(CANARY_PHONE));
    }

    @Test
    void removesExpiredEntriesAndEvictsTheOldestEntryAtCapacity() {
        MutableClock clock = new MutableClock(START);
        InMemoryLocalSmsInboxAdapter inbox = inbox(clock, 2);
        LocalSmsInboxPort.Entry expired = entry(1, START.plusSeconds(1), "111111");

        inbox.store(expired);
        clock.advance(Duration.ofSeconds(1));

        assertTrue(inbox.findRecent(10).isEmpty());
        assertTrue(inbox.consume(expired.challengeId()).isEmpty());

        LocalSmsInboxPort.Entry first = entry(2, START.plusSeconds(300), "222222");
        LocalSmsInboxPort.Entry second = entry(3, START.plusSeconds(300), "333333");
        LocalSmsInboxPort.Entry third = entry(4, START.plusSeconds(300), "444444");
        inbox.store(first);
        inbox.store(second);
        inbox.store(third);

        assertTrue(inbox.consume(first.challengeId()).isEmpty());
        assertEquals(
                List.of(third.challengeId(), second.challengeId()),
                inbox.findRecent(10).stream().map(LocalSmsInboxPort.Entry::challengeId).toList()
        );
    }

    @Test
    void acceptsIdempotentDuplicateButRejectsDifferentOtpForTheSameChallenge() {
        MutableClock clock = new MutableClock(START);
        InMemoryLocalSmsInboxAdapter inbox = inbox(clock, 10);
        LocalSmsInboxPort.Entry original = entry(1, START.plusSeconds(300), CANARY_OTP);

        inbox.store(original);
        clock.advance(Duration.ofSeconds(1));
        inbox.store(new LocalSmsInboxPort.Entry(
                original.eventId(),
                original.challengeId(),
                original.otp(),
                original.maskedDestination(),
                original.templateKey(),
                original.purpose(),
                clock.instant(),
                original.expiresAt()
        ));

        assertEquals(1, inbox.findRecent(10).size());
        assertThrows(
                IllegalStateException.class,
                () -> inbox.store(entry(1, START.plusSeconds(300), "000000"))
        );
    }

    @Test
    void concurrentConsumersReceiveTheOtpAtMostOnce() throws Exception {
        MutableClock clock = new MutableClock(START);
        InMemoryLocalSmsInboxAdapter inbox = inbox(clock, 10);
        LocalSmsInboxPort.Entry message = entry(1, START.plusSeconds(300), CANARY_OTP);
        inbox.store(message);

        int consumers = 16;
        CountDownLatch ready = new CountDownLatch(consumers);
        CountDownLatch start = new CountDownLatch(1);
        List<Boolean> results = new ArrayList<>();

        try (var executor = Executors.newFixedThreadPool(consumers)) {
            for (int i = 0; i < consumers; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    boolean consumed = inbox.consume(message.challengeId()).isPresent();
                    synchronized (results) {
                        results.add(consumed);
                    }
                    return null;
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    }

    private InMemoryLocalSmsInboxAdapter inbox(Clock clock, int capacity) {
        return new InMemoryLocalSmsInboxAdapter(
                clock,
                new LocalSmsInboxProperties("a-local-test-token-at-least-24-characters", capacity)
        );
    }

    private LocalSmsInboxPort.Entry entry(int sequence, Instant expiresAt, String otp) {
        return new LocalSmsInboxPort.Entry(
                new UUID(0, sequence),
                new UUID(1, sequence),
                otp,
                "********" + CANARY_PHONE.substring(CANARY_PHONE.length() - 4),
                "OTP_SMS",
                "SIGNUP_PHONE",
                START,
                expiresAt
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
