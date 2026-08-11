package com.timeeconomy.notification.application.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.timeeconomy.contracts.auth.v1.VerificationOtpDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalOtpClientPort;
import com.timeeconomy.notification.application.notification.port.out.EmailSenderPort;
import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;
import com.timeeconomy.notification.domain.inbox.port.out.ProcessedEventRepositoryPort;
import com.timeeconomy.notification.domain.notification.model.NotificationDelivery;
import com.timeeconomy.notification.domain.notification.model.NotificationStatus;
import com.timeeconomy.notification.domain.notification.port.out.NotificationDeliveryRepositoryPort;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HandleVerificationOtpDeliveryRequestedServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CHALLENGE_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-09T12:00:00Z");
    private static final int TTL_SECONDS = 600;
    private static final String CANARY_PHONE = "+821012345678";
    private static final String CANARY_OTP = "731942";

    @Mock
    private ProcessedEventRepositoryPort processedEventRepositoryPort;
    @Mock
    private NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;
    @Mock
    private AuthInternalOtpClientPort authInternalOtpClientPort;
    @Mock
    private EmailSenderPort emailSenderPort;
    @Mock
    private SmsSenderPort smsSenderPort;

    @InjectMocks
    private HandleVerificationOtpDeliveryRequestedService service;

    @Test
    void mapsTheSmsEventToATypedSecretSafeCommand(CapturedOutput output) {
        VerificationOtpDeliveryRequestedV1 event = VerificationOtpDeliveryRequestedV1.newBuilder()
                .setEventId(EVENT_ID.toString())
                .setOccurredAtEpochMillis(OCCURRED_AT.toEpochMilli())
                .setVerificationChallengeId(CHALLENGE_ID.toString())
                .setPurpose("SIGNUP_PHONE")
                .setChannel("SMS")
                .setSubjectType("SIGNUP_SESSION")
                .setSubjectId("30000000-0000-0000-0000-000000000003")
                .setDestinationNorm(CANARY_PHONE)
                .setTtlSeconds(TTL_SECONDS)
                .build();
        ConsumerContext context = new ConsumerContext(
                "notification-service",
                "auth.verification.otp.delivery.requested.v1",
                1,
                42L,
                OCCURRED_AT.toEpochMilli(),
                EVENT_ID.toString(),
                null,
                EVENT_ID,
                "VerificationOtpDeliveryRequested.v1"
        );
        when(processedEventRepositoryPort.markProcessed(any(ProcessedEvent.class))).thenReturn(true);
        when(authInternalOtpClientPort.getOtpOnce(CHALLENGE_ID)).thenReturn(Optional.of(CANARY_OTP));
        when(smsSenderPort.sendTemplate(any(SmsSenderPort.SmsSendCommand.class)))
                .thenReturn(new SmsSenderPort.SmsSendResult("MOCK", "local-" + EVENT_ID));

        service.handle(event, context);

        ArgumentCaptor<SmsSenderPort.SmsSendCommand> commandCaptor =
                ArgumentCaptor.forClass(SmsSenderPort.SmsSendCommand.class);
        verify(smsSenderPort).sendTemplate(commandCaptor.capture());
        SmsSenderPort.SmsSendCommand command = commandCaptor.getValue();
        assertEquals(EVENT_ID, command.eventId());
        assertEquals(CHALLENGE_ID, command.challengeId());
        assertEquals("OTP_SMS", command.templateKey());
        assertEquals(CANARY_PHONE, command.toPhone());
        assertEquals(CANARY_OTP, command.otp());
        assertEquals("SIGNUP_PHONE", command.purpose());
        assertEquals(OCCURRED_AT.plusSeconds(TTL_SECONDS), command.expiresAt());

        ArgumentCaptor<NotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepositoryPort).save(deliveryCaptor.capture());
        assertEquals(NotificationStatus.SENT, deliveryCaptor.getValue().getStatus());
        assertEquals("MOCK", deliveryCaptor.getValue().getProvider());
        verify(emailSenderPort, never()).sendTemplate(any());
        assertFalse(output.getAll().contains(CANARY_PHONE));
        assertFalse(output.getAll().contains(CANARY_OTP));
    }
}
