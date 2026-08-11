package com.timeeconomy.notification.adapter.out.jpa.inbox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.timeeconomy.notification.adapter.out.jpa.inbox.repository.ProcessedEventJpaRepository;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;

@ExtendWith(MockitoExtension.class)
class ProcessedEventJpaAdapterTest {

    private static final String CONSUMER_GROUP = "notification-service";
    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String EVENT_TYPE = "VerificationOtpDeliveryRequested.v1";
    private static final String TOPIC = "auth.verification.otp.delivery.requested.v1";
    private static final int PARTITION = 2;
    private static final long OFFSET = 42L;
    private static final Instant PROCESSED_AT = Instant.parse("2026-08-11T10:00:00Z");

    @Mock
    private ProcessedEventJpaRepository repository;

    private ProcessedEventJpaAdapter adapter;
    private ProcessedEvent processedEvent;

    @BeforeEach
    void setUp() {
        adapter = new ProcessedEventJpaAdapter(repository);
        processedEvent = ProcessedEvent.newProcessed(
                CONSUMER_GROUP,
                EVENT_ID,
                EVENT_TYPE,
                TOPIC,
                PARTITION,
                OFFSET,
                PROCESSED_AT
        );
    }

    @Test
    void returnsTrueWhenTheNativeInsertCreatesTheGateRow() {
        whenInsertReturns(1);

        assertTrue(adapter.markProcessed(processedEvent));

        verifyInsert();
    }

    @Test
    void returnsFalseWhenTheEventAlreadyExists() {
        whenInsertReturns(0);

        assertFalse(adapter.markProcessed(processedEvent));

        verifyInsert();
    }

    @Test
    void rejectsAnUnexpectedNativeUpdateCount() {
        whenInsertReturns(2);

        assertThrows(IllegalStateException.class, () -> adapter.markProcessed(processedEvent));
    }

    private void whenInsertReturns(int insertedRows) {
        when(repository.insertIfAbsent(
                CONSUMER_GROUP,
                EVENT_ID,
                EVENT_TYPE,
                TOPIC,
                PARTITION,
                OFFSET,
                PROCESSED_AT
        )).thenReturn(insertedRows);
    }

    private void verifyInsert() {
        verify(repository).insertIfAbsent(
                CONSUMER_GROUP,
                EVENT_ID,
                EVENT_TYPE,
                TOPIC,
                PARTITION,
                OFFSET,
                PROCESSED_AT
        );
    }
}
