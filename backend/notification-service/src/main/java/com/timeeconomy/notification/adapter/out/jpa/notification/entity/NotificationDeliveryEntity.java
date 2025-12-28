package com.timeeconomy.notification.adapter.out.jpa.notification.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_notification_delivery", columnNames = {"event_id", "template"})
        }
)
public class NotificationDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ DB: UUID
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    // Keep as String columns (maps well to enums in domain via mapper)
    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "template", nullable = false, length = 100)
    private String template;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_msg_id", length = 200)
    private String providerMsgId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NotificationDeliveryEntity() {}

    public Long getId() { return id; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderMsgId() { return providerMsgId; }
    public void setProviderMsgId(String providerMsgId) { this.providerMsgId = providerMsgId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}