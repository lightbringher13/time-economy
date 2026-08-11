package com.timeeconomy.notification.adapter.out.jpa.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;
import com.timeeconomy.notification.domain.inbox.port.out.ProcessedEventRepositoryPort;

@SpringBootTest
@ActiveProfiles({"test", "sms-mock"})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProcessedEventJpaAdapterIntegrationTest {

    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine")
    );

    @Autowired
    private ProcessedEventRepositoryPort processedEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearProcessedEvents() {
        jdbcTemplate.update("DELETE FROM processed_events");
    }

    @Test
    void duplicateGateDoesNotMarkTheOuterTransactionRollbackOnly() {
        ProcessedEvent processedEvent = ProcessedEvent.newProcessed(
                "notification-service",
                EVENT_ID,
                "VerificationOtpDeliveryRequested.v1",
                "auth.verification.otp.delivery.requested.v1",
                2,
                42L,
                Instant.parse("2026-08-11T10:00:00Z")
        );

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertTrue(processedEventRepository.markProcessed(processedEvent));
            assertFalse(processedEventRepository.markProcessed(processedEvent));
        });

        Long committedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE consumer_group = ? AND event_id = ?",
                Long.class,
                processedEvent.getConsumerGroup(),
                processedEvent.getEventId()
        );
        assertEquals(1L, committedRows);
    }
}
