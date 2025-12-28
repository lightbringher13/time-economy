package com.timeeconomy.notification.domain.notification.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class NotificationDelivery {

    private Long id;

    private final UUID eventId;
    private final String eventType;

    private final NotificationChannel channel; // EMAIL | SMS | PUSH
    private final String template;

    private final String recipient;

    private final NotificationStatus status;   // SENT | FAILED
    private final String provider;
    private final String providerMsgId;

    private final String errorMessage;

    private final Instant createdAt;

    private NotificationDelivery(
            Long id,
            UUID eventId,
            String eventType,
            NotificationChannel channel,
            String template,
            String recipient,
            NotificationStatus status,
            String provider,
            String providerMsgId,
            String errorMessage,
            Instant createdAt
    ) {
        this.id = id;

        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");

        this.channel = Objects.requireNonNull(channel, "channel");
        this.template = Objects.requireNonNull(template, "template");

        this.recipient = Objects.requireNonNull(recipient, "recipient");

        this.status = Objects.requireNonNull(status, "status");
        this.provider = provider;
        this.providerMsgId = providerMsgId;

        this.errorMessage = errorMessage;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static NotificationDelivery sent(
            UUID eventId,
            String eventType,
            NotificationChannel channel,
            String template,
            String recipient,
            String provider,
            String providerMsgId,
            Instant now
    ) {
        Instant ts = (now != null ? now : Instant.now());
        return new NotificationDelivery(
                null,
                eventId,
                eventType,
                channel,
                template,
                recipient,
                NotificationStatus.SENT,
                provider,
                providerMsgId,
                null,
                ts
        );
    }

    public static NotificationDelivery failed(
            UUID eventId,
            String eventType,
            NotificationChannel channel,
            String template,
            String recipient,
            String provider,
            String errorMessage,
            Instant now
    ) {
        Instant ts = (now != null ? now : Instant.now());
        return new NotificationDelivery(
                null,
                eventId,
                eventType,
                channel,
                template,
                recipient,
                NotificationStatus.FAILED,
                provider,
                null,
                errorMessage,
                ts
        );
    }

    // getters
    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public NotificationChannel getChannel() { return channel; }
    public String getTemplate() { return template; }
    public String getRecipient() { return recipient; }
    public NotificationStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderMsgId() { return providerMsgId; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}