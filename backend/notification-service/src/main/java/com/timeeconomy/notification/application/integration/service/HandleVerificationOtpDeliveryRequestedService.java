package com.timeeconomy.notification.application.integration.service;

import com.timeeconomy.contracts.auth.v1.VerificationOtpDeliveryRequestedV1;
import com.timeeconomy.notification.adapter.in.kafka.dto.ConsumerContext;
import com.timeeconomy.notification.application.integration.port.in.HandleVerificationOtpDeliveryRequestedUseCase;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalOtpClientPort;
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
public class HandleVerificationOtpDeliveryRequestedService
        implements HandleVerificationOtpDeliveryRequestedUseCase {

    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;

    // You can refine this mapping later (per purpose/channel)
    private static final String TEMPLATE_KEY = "OTP_EMAIL";

    private final ProcessedEventRepositoryPort processedEventRepositoryPort;
    private final NotificationDeliveryRepositoryPort notificationDeliveryRepositoryPort;

    private final AuthInternalOtpClientPort authInternalOtpClientPort;
    private final EmailSenderPort emailSenderPort;

    @Override
    @Transactional
    public void handle(VerificationOtpDeliveryRequestedV1 event, ConsumerContext ctx) {
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

        // 2) fetch OTP once (internal HTTP)
        final UUID challengeId = UUID.fromString(event.getVerificationChallengeId());
        Optional<String> otpOpt = authInternalOtpClientPort.getOtpOnce(challengeId);

        if (otpOpt.isEmpty()) {
            // ✅ treat as NON-retryable: OTP is already consumed/expired; resend will create new OTP
            notificationDeliveryRepositoryPort.save(
                    NotificationDelivery.failed(
                            eventId,
                            eventType,
                            CHANNEL,
                            TEMPLATE_KEY,
                            toStr(event.getDestinationNorm()),
                            "AUTH_INTERNAL",
                            "OTP_NOT_FOUND_OR_ALREADY_CONSUMED",
                            now
                    )
            );

            log.warn("[SKIP] otp missing (non-retryable). eventId={} challengeId={}",
                    eventId, challengeId);
            return;
        }

        final String otp = otpOpt.get();

        // 3) send email
        final String recipientEmail = toStr(event.getDestinationNorm());
        final String purpose = toStr(event.getPurpose());
        final int ttlSeconds = event.getTtlSeconds();

        try {
            var cmd = new EmailSenderPort.EmailSendCommand(
                    TEMPLATE_KEY,
                    recipientEmail,
                    /*toName*/ null,
                    Map.of(
                            "otp", otp,
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

            log.info("[OK] otp email sent+persisted template={} to={} provider={} msgId={}",
                    TEMPLATE_KEY, recipientEmail, res.provider(), res.providerMsgId());

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