package com.timeeconomy.notification.application.integration.service;

import com.timeeconomy.contracts.auth.v2.AuthUserRegisteredV2;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleAuthUserRegisteredUseCase;
import com.timeeconomy.notification.application.integration.port.out.SignupSessionInternalClientPort;
import com.timeeconomy.notification.adapter.out.authclient.dto.CompletedSignupSessionResponse;
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
import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleAuthUserRegisteredService implements HandleAuthUserRegisteredUseCase {

    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;
    private static final String TEMPLATE_KEY = "WELCOME_EMAIL";

    private final ProcessedEventRepositoryPort processedEventRepositoryPort;
    private final NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final SignupSessionInternalClientPort signupSessionInternalClientPort;

    private final Clock clock;

    @Override
    @Transactional
    public void handle(AuthUserRegisteredV2 event, ConsumerContext ctx) {
        final Instant now = Instant.now(clock);

        final UUID eventId = event.getEventId();
        final String eventType = ctx.eventType(); // from headers
        final long userId = event.getUserId();

        final UUID signupSessionId = event.getSignupSessionId(); 

        final Instant occurredAt = event.getOccurredAtEpochMillis();

        CompletedSignupSessionResponse session =
                signupSessionInternalClientPort.getCompletedSession(signupSessionId);

        // defensive: if internal endpoint accidentally returns non-completed
        if (!"COMPLETED".equals(session.state())) {
            throw new IllegalStateException("SignupSession not COMPLETED. sessionId=" + signupSessionId +
                    " state=" + session.state());
        }

        final String recipientEmail = session.email();
        final String toName = session.name(); // nullable ok

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

        // 2) send email
        try {
            var cmd = new EmailSenderPort.EmailSendCommand(
                    TEMPLATE_KEY,
                    recipientEmail,
                    toName,
                    Map.of(
                            "userId", userId,
                            "email", recipientEmail,
                            "name", toName,
                            "phoneNumber", session.phoneNumber(),
                            "gender", session.gender(),
                            "birthDate", session.birthDate(), // string
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

            // throw -> listener does NOT ack -> Kafka retry (if configured)
            throw ex;
        }
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 500 ? m.substring(0, 500) : m;
    }
}