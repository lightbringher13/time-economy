package com.timeeconomy.notification.application.integration.service;

import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleAuthUserRegisteredUseCase;
import com.timeeconomy.notification.application.notification.port.out.EmailSenderPort;
import com.timeeconomy.notification.domain.inbox.model.ProcessedEvent;
import com.timeeconomy.notification.domain.inbox.port.out.ProcessedEventRepositoryPort;
import com.timeeconomy.notification.domain.notification.model.NotificationChannel;
import com.timeeconomy.notification.domain.notification.model.NotificationDelivery;
import com.timeeconomy.notification.domain.notification.port.out.NotificationDeliveryRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleAuthUserRegisteredService implements HandleAuthUserRegisteredUseCase {

    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;
    private static final String TEMPLATE_KEY = "WELCOME_EMAIL";

    private final ProcessedEventRepositoryPort processedEventRepositoryPort;
    private final NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;
    private final EmailSenderPort emailSenderPort;

    @Override
    @Transactional
    public void handle(AuthUserRegisteredV1 event, ConsumerContext ctx) {
        final Instant now = Instant.now();

        final UUID eventId = event.getEventId();
        final String eventType = ctx.eventType();     // from headers
        final long userId = event.getUserId();

        final String recipientEmail = toStr(event.getEmail());
        final String toName = toStr(event.getName()); // nullable in schema
        final Instant occurredAt = event.getOccurredAtEpochMillis();

        // 1) idempotency gate
        ProcessedEvent processed = ProcessedEvent.newProcessed(
                ctx.consumerGroup(),
                eventId,
                eventType,
                ctx.topic(),
                ctx.partition(),
                ctx.offset(),
                now
        );

        if (!processedEventRepositoryPort.markProcessed(processed)) {
            log.info("[SKIP] duplicate eventId={} group={} topic={} p={} o={}",
                    eventId, ctx.consumerGroup(), ctx.topic(), ctx.partition(), ctx.offset());
            return;
        }

        // 2) send email (Brevo via port)
        try {
            var cmd = new EmailSenderPort.EmailSendCommand(
                    TEMPLATE_KEY,
                    recipientEmail,
                    toName,
                    Map.of(
                            "userId", userId,
                            "email", recipientEmail,
                            "name", toName,
                            "occurredAt", String.valueOf(occurredAt)
                    )
            );

            EmailSenderPort.EmailSendResult res = emailSenderPort.sendTemplate(cmd);

            // 3) persist delivery as SENT
            notificationDeliveryRepositoryPort.save(
                    NotificationDelivery.sent(
                            eventId,
                            eventType,
                            CHANNEL,
                            TEMPLATE_KEY,
                            recipientEmail,
                            res.provider(),
                            res.providerMsgId(),
                            now
                    )
            );

            log.info("[OK] welcome email sent+persisted userId={} email={} provider={} msgId={}",
                    userId, recipientEmail, res.provider(), res.providerMsgId());

        } catch (Exception ex) {
            // persist delivery as FAILED
            notificationDeliveryRepositoryPort.save(
                    NotificationDelivery.failed(
                            eventId,
                            eventType,
                            CHANNEL,
                            TEMPLATE_KEY,
                            recipientEmail,
                            "BREVO",
                            safeMsg(ex),
                            now
                    )
            );

            // throw -> listener does NOT ack -> Kafka retry
            throw ex;
        }
    }

    private static String toStr(Object v) {
        return v == null ? null : v.toString();
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 500 ? m.substring(0, 500) : m;
    }
}