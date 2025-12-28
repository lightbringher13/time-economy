package com.timeeconomy.notification.application.integration.service;

import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleEmailChangeCommittedUseCase;
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
public class HandleEmailChangeCommittedService implements HandleEmailChangeCommittedUseCase {

    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;

    // pick names that match your adapter's switch-case keys
    private static final String TEMPLATE_KEY_NEW = "EMAIL_CHANGE_NEW";
    private static final String TEMPLATE_KEY_OLD = "EMAIL_CHANGE_OLD";

    private final ProcessedEventRepositoryPort processedEventRepositoryPort;
    private final NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;
    private final EmailSenderPort emailSenderPort;

    @Override
    @Transactional
    public void handle(EmailChangeCommittedV1 event, ConsumerContext ctx) {
        final Instant now = Instant.now();

        final UUID eventId = event.getEventId();
        final String eventType = ctx.eventType(); // from header

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

        // 2) send emails
        // policy: notify BOTH old + new (recommended for security). If you want only new, delete the old block.
        final String oldEmail = toStr(event.getOldEmail());
        final String newEmail = toStr(event.getNewEmail());
        final Long userId = event.getUserId(); // long in schema? adjust if needed

        // optional occurredAt (depends on your schema getter type)
        final Object occurredAt = event.getOccurredAtEpochMillis();

        // send to NEW email
        sendAndPersist(
                eventId,
                eventType,
                now,
                TEMPLATE_KEY_NEW,
                newEmail,
                Map.of(
                        "userId", userId,
                        "oldEmail", oldEmail,
                        "newEmail", newEmail,
                        "occurredAt", String.valueOf(occurredAt)
                )
        );

        // send to OLD email (if present)
        if (oldEmail != null && !oldEmail.isBlank()) {
            sendAndPersist(
                    eventId,
                    eventType,
                    now,
                    TEMPLATE_KEY_OLD,
                    oldEmail,
                    Map.of(
                            "userId", userId,
                            "oldEmail", oldEmail,
                            "newEmail", newEmail,
                            "occurredAt", String.valueOf(occurredAt)
                    )
            );
        }
    }

    private void sendAndPersist(
            UUID eventId,
            String eventType,
            Instant now,
            String templateKey,
            String recipientEmail,
            Map<String, Object> params
    ) {
        try {
            var cmd = new EmailSenderPort.EmailSendCommand(
                    templateKey,
                    recipientEmail,
                    /*toName*/ null,
                    params
            );

            EmailSenderPort.EmailSendResult res = emailSenderPort.sendTemplate(cmd);

            notificationDeliveryRepositoryPort.save(
                    NotificationDelivery.sent(
                            eventId,
                            eventType,
                            CHANNEL,
                            templateKey,
                            recipientEmail,
                            res.provider(),
                            res.providerMsgId(),
                            now
                    )
            );

            log.info("[OK] email-change email sent+persisted template={} to={} provider={} msgId={}",
                    templateKey, recipientEmail, res.provider(), res.providerMsgId());

        } catch (Exception ex) {
            notificationDeliveryRepositoryPort.save(
                    NotificationDelivery.failed(
                            eventId,
                            eventType,
                            CHANNEL,
                            templateKey,
                            recipientEmail,
                            "BREVO",
                            safeMsg(ex),
                            now
                    )
            );
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