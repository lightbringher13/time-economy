package com.timeeconomy.notification.application.integration.service;

import com.timeeconomy.contracts.auth.v1.VerificationLinkDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleVerificationLinkDeliveryRequestedUseCase;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalLinkClientPort;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleVerificationLinkDeliveryRequestedService
        implements HandleVerificationLinkDeliveryRequestedUseCase {

    // If you want this dynamic later, map from event.getChannel()
    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;

    // You can refine this mapping later (per purpose/channel)
    private static final String TEMPLATE_KEY = "LINK_EMAIL";

    private final ProcessedEventRepositoryPort processedEventRepositoryPort;
    private final NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;

    private final AuthInternalLinkClientPort authInternalLinkClientPort;
    private final EmailSenderPort emailSenderPort;

    @Override
    @Transactional
    public void handle(VerificationLinkDeliveryRequestedV1 event, ConsumerContext ctx) {
        final Instant now = Instant.now();

        final UUID eventId = UUID.fromString(event.getEventId());
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

        // 2) fetch Link URL once (internal HTTP)
        final UUID challengeId = UUID.fromString(event.getVerificationChallengeId());
        final String purpose = toStr(event.getPurpose());

        Optional<String> linkUrlOpt = authInternalLinkClientPort.getLinkUrlOnce(challengeId, purpose, eventId);

        if (linkUrlOpt.isEmpty()) {
            // NON-retryable: token already consumed/expired; user must request a new link
            notificationDeliveryRepositoryPort.save(
                    NotificationDelivery.failed(
                            eventId,
                            eventType,
                            CHANNEL,
                            TEMPLATE_KEY,
                            toStr(event.getDestinationNorm()),
                            "AUTH_INTERNAL",
                            "LINK_URL_NOT_FOUND_OR_ALREADY_CONSUMED",
                            now
                    )
            );

            log.warn("[SKIP] linkUrl missing (non-retryable). eventId={} challengeId={}",
                    eventId, challengeId);
            return;
        }

        final String linkUrl = linkUrlOpt.get();

        // 3) send email
        final String recipientEmail = toStr(event.getDestinationNorm());
        final int ttlSeconds = event.getTtlSeconds();

        try {
            var cmd = new EmailSenderPort.EmailSendCommand(
                    TEMPLATE_KEY,
                    recipientEmail,
                    /*toName*/ null,
                    Map.of(
                            "linkUrl", linkUrl,
                            "ttlSeconds", ttlSeconds,
                            "purpose", purpose,
                            "challengeId", challengeId.toString()
                    )
            );

            EmailSenderPort.EmailSendResult res = emailSenderPort.sendTemplate(cmd);

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

            log.info("[OK] link email sent+persisted template={} to={} provider={} msgId={} challengeId={}",
                    TEMPLATE_KEY, recipientEmail, res.provider(), res.providerMsgId(), challengeId);

        } catch (Exception ex) {
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
            throw ex; // retryable
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