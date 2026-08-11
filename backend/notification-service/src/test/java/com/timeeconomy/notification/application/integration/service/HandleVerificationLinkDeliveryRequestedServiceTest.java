package com.timeeconomy.notification.application.integration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.timeeconomy.contracts.auth.v1.VerificationLinkDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.exception.VerificationLinkDeliveryRetryException;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalLinkClientPort;
import com.timeeconomy.notification.application.notification.port.out.EmailSenderPort;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;
import com.timeeconomy.notification.domain.inbox.port.out.ProcessedEventRepositoryPort;
import com.timeeconomy.notification.domain.notification.model.NotificationChannel;
import com.timeeconomy.notification.domain.notification.model.NotificationDelivery;
import com.timeeconomy.notification.domain.notification.model.NotificationStatus;
import com.timeeconomy.notification.domain.notification.port.out.NotificationDeliveryRepositoryPort;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HandleVerificationLinkDeliveryRequestedServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000011");
    private static final UUID CHALLENGE_ID = UUID.fromString("20000000-0000-0000-0000-000000000022");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-11T03:00:00Z");
    private static final int TTL_SECONDS = 900;
    private static final String PURPOSE = "SIGNUP_EMAIL";
    private static final String CANARY_EMAIL = "link-canary@example.test";
    private static final String CANARY_LINK = "https://example.test/verify?token=link-canary-secret";

    @Mock
    private ProcessedEventRepositoryPort processedEventRepositoryPort;
    @Mock
    private NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;
    @Mock
    private AuthInternalLinkClientPort authInternalLinkClientPort;
    @Mock
    private EmailSenderPort emailSenderPort;
    @Mock
    private LinkDeliveryAcknowledgmentCoordinator linkDeliveryAcknowledgmentCoordinator;

    @InjectMocks
    private HandleVerificationLinkDeliveryRequestedService service;

    @Test
    void mapsEmailEventToTypedCommandWithoutLoggingRecipientOrLink(CapturedOutput output) {
        stubSuccessfulDelivery();

        service.handle(linkEvent("EMAIL"), context());

        ArgumentCaptor<EmailSenderPort.EmailSendCommand> commandCaptor =
                ArgumentCaptor.forClass(EmailSenderPort.EmailSendCommand.class);
        verify(emailSenderPort).sendTemplate(commandCaptor.capture());
        EmailSenderPort.EmailSendCommand command = commandCaptor.getValue();
        assertEquals("LINK_EMAIL", command.templateKey());
        assertEquals(CANARY_EMAIL, command.toEmail());
        assertNull(command.toName());
        assertEquals(
                Map.of(
                        "linkUrl", CANARY_LINK,
                        "ttlSeconds", TTL_SECONDS,
                        "purpose", PURPOSE,
                        "challengeId", CHALLENGE_ID.toString()
                ),
                command.params()
        );

        ArgumentCaptor<NotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepositoryPort).save(deliveryCaptor.capture());
        NotificationDelivery delivery = deliveryCaptor.getValue();
        assertEquals(NotificationStatus.SENT, delivery.getStatus());
        assertEquals(NotificationChannel.EMAIL, delivery.getChannel());
        assertEquals(CANARY_EMAIL, delivery.getRecipient());
        assertEquals("MOCK", delivery.getProvider());

        verify(authInternalLinkClientPort).peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID);
        verify(linkDeliveryAcknowledgmentCoordinator).acknowledgeAfterCommit(EVENT_ID, CHALLENGE_ID);
        assertFalse(output.getAll().contains(CANARY_EMAIL));
        assertFalse(output.getAll().contains(CANARY_LINK));
    }

    @Test
    void acknowledgesLinkOnlyAfterTheTransactionCommits() {
        stubSuccessfulDelivery();
        HandleVerificationLinkDeliveryRequestedService transactionalService = serviceWithRealCoordinator();

        transactionTemplate().executeWithoutResult(status -> {
            transactionalService.handle(linkEvent("EMAIL"), context());
            verify(authInternalLinkClientPort, never()).acknowledgeLink(any());
        });

        verify(authInternalLinkClientPort, times(1)).acknowledgeLink(CHALLENGE_ID);
    }

    @Test
    void doesNotAcknowledgeLinkWhenTheTransactionRollsBack() {
        stubSuccessfulDelivery();
        HandleVerificationLinkDeliveryRequestedService transactionalService = serviceWithRealCoordinator();

        transactionTemplate().executeWithoutResult(status -> {
            transactionalService.handle(linkEvent("EMAIL"), context());
            status.setRollbackOnly();
        });

        verify(authInternalLinkClientPort, never()).acknowledgeLink(any());
    }

    @Test
    void providerFailureLeavesLinkUnacknowledgedAndDoesNotLogSecrets(CapturedOutput output) {
        when(processedEventRepositoryPort.markProcessed(any(ProcessedEvent.class))).thenReturn(true);
        when(authInternalLinkClientPort.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID))
                .thenReturn(Optional.of(CANARY_LINK));
        when(emailSenderPort.sendTemplate(any(EmailSenderPort.EmailSendCommand.class)))
                .thenThrow(new IllegalStateException("provider failed " + CANARY_EMAIL + " " + CANARY_LINK));
        HandleVerificationLinkDeliveryRequestedService transactionalService = serviceWithRealCoordinator();

        VerificationLinkDeliveryRetryException retryException = assertThrows(
                VerificationLinkDeliveryRetryException.class,
                () -> transactionTemplate().executeWithoutResult(
                        status -> transactionalService.handle(linkEvent("EMAIL"), context())
                )
        );

        assertNull(retryException.getCause());
        assertFalse(retryException.getMessage().contains(CANARY_EMAIL));
        assertFalse(retryException.getMessage().contains(CANARY_LINK));
        assertFalse(retryException.toString().contains(CANARY_EMAIL));
        assertFalse(retryException.toString().contains(CANARY_LINK));
        verify(authInternalLinkClientPort, never()).acknowledgeLink(any());
        ArgumentCaptor<NotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepositoryPort).save(deliveryCaptor.capture());
        NotificationDelivery failedDelivery = deliveryCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, failedDelivery.getStatus());
        assertEquals("IllegalStateException", failedDelivery.getErrorMessage());
        assertFalse(failedDelivery.getErrorMessage().contains(CANARY_EMAIL));
        assertFalse(failedDelivery.getErrorMessage().contains(CANARY_LINK));
        assertFalse(output.getAll().contains(CANARY_EMAIL));
        assertFalse(output.getAll().contains(CANARY_LINK));
    }

    @Test
    void deliveryPersistenceFailureLeavesLinkUnacknowledged() {
        when(processedEventRepositoryPort.markProcessed(any(ProcessedEvent.class))).thenReturn(true);
        when(authInternalLinkClientPort.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID))
                .thenReturn(Optional.of(CANARY_LINK));
        when(emailSenderPort.sendTemplate(any(EmailSenderPort.EmailSendCommand.class)))
                .thenReturn(new EmailSenderPort.EmailSendResult(
                        "MOCK",
                        "provider-message-" + CANARY_EMAIL + "-" + CANARY_LINK
                ));
        when(notificationDeliveryRepositoryPort.save(any(NotificationDelivery.class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        HandleVerificationLinkDeliveryRequestedService transactionalService = serviceWithRealCoordinator();

        assertThrows(
                IllegalStateException.class,
                () -> transactionTemplate().executeWithoutResult(
                        status -> transactionalService.handle(linkEvent("EMAIL"), context())
                )
        );

        verify(authInternalLinkClientPort, never()).acknowledgeLink(any());
    }

    @Test
    void acknowledgmentFailureDoesNotFailCommittedDeliveryOrExposeSecrets(CapturedOutput output) {
        stubSuccessfulDelivery();
        doThrow(new IllegalStateException("ack failed " + CANARY_EMAIL + " " + CANARY_LINK))
                .when(authInternalLinkClientPort)
                .acknowledgeLink(CHALLENGE_ID);
        HandleVerificationLinkDeliveryRequestedService transactionalService = serviceWithRealCoordinator();

        assertDoesNotThrow(() -> transactionTemplate().executeWithoutResult(
                status -> transactionalService.handle(linkEvent("EMAIL"), context())
        ));

        verify(authInternalLinkClientPort).acknowledgeLink(CHALLENGE_ID);
        assertFalse(output.getAll().contains(CANARY_EMAIL));
        assertFalse(output.getAll().contains(CANARY_LINK));
    }

    @Test
    void rejectsNonEmailChannelBeforeFetchingSendingOrAcknowledging() {
        HandleVerificationLinkDeliveryRequestedService transactionalService = serviceWithRealCoordinator();

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionalService.handle(linkEvent("SMS"), context())
        );

        verifyNoInteractions(
                processedEventRepositoryPort,
                notificationDeliveryRepositoryPort,
                authInternalLinkClientPort,
                emailSenderPort
        );
    }

    private void stubSuccessfulDelivery() {
        when(processedEventRepositoryPort.markProcessed(any(ProcessedEvent.class))).thenReturn(true);
        when(authInternalLinkClientPort.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID))
                .thenReturn(Optional.of(CANARY_LINK));
        when(emailSenderPort.sendTemplate(any(EmailSenderPort.EmailSendCommand.class)))
                .thenReturn(new EmailSenderPort.EmailSendResult(
                        "MOCK",
                        "provider-message-" + CANARY_EMAIL + "-" + CANARY_LINK
                ));
    }

    private HandleVerificationLinkDeliveryRequestedService serviceWithRealCoordinator() {
        return new HandleVerificationLinkDeliveryRequestedService(
                processedEventRepositoryPort,
                notificationDeliveryRepositoryPort,
                authInternalLinkClientPort,
                emailSenderPort,
                new LinkDeliveryAcknowledgmentCoordinator(authInternalLinkClientPort)
        );
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new TestTransactionManager());
    }

    private VerificationLinkDeliveryRequestedV1 linkEvent(String channel) {
        return VerificationLinkDeliveryRequestedV1.newBuilder()
                .setEventId(EVENT_ID.toString())
                .setOccurredAtEpochMillis(OCCURRED_AT.toEpochMilli())
                .setVerificationChallengeId(CHALLENGE_ID.toString())
                .setPurpose(PURPOSE)
                .setChannel(channel)
                .setSubjectType("SIGNUP_SESSION")
                .setSubjectId("30000000-0000-0000-0000-000000000033")
                .setDestinationNorm(CANARY_EMAIL)
                .setTtlSeconds(TTL_SECONDS)
                .build();
    }

    private ConsumerContext context() {
        return new ConsumerContext(
                "notification-service",
                "auth.verification.link.delivery.requested.v1",
                2,
                43L,
                OCCURRED_AT.toEpochMilli(),
                EVENT_ID.toString(),
                null,
                EVENT_ID,
                "VerificationLinkDeliveryRequested.v1"
        );
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
