package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;
import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;
import org.apache.avro.specific.SpecificRecord;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

public final class JacksonOutboxAvroMapper implements OutboxAvroMapper {

    private final JsonMapper jsonMapper;

    public JacksonOutboxAvroMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public SpecificRecord toAvro(OutboxEvent event) {
        return switch (event.getEventType()) {
            case "AuthUserRegistered.v1" -> mapAuthUserRegisteredV1(event);
            case "EmailChangeCommitted.v1" -> mapEmailChangeCommittedV1(event);
            default -> throw new IllegalArgumentException("Unsupported eventType=" + event.getEventType());
        };
    }

    private AuthUserRegisteredV1 mapAuthUserRegisteredV1(OutboxEvent event) {
        AuthUserRegisteredPayload p = read(event.getPayload(), AuthUserRegisteredPayload.class);

        return AuthUserRegisteredV1.newBuilder()
                // AVSC: eventId = string logicalType uuid  -> UUID
                .setEventId(UUID.fromString(event.getId().toString()))
                // AVSC: occurredAtEpochMillis = long logicalType timestamp-millis -> Instant
                .setOccurredAtEpochMillis(toInstantUtc(event.getOccurredAt()))
                .setUserId(p.userId())
                .setEmail(p.email())
                .setPhoneNumber(p.phoneNumber())
                .setName(p.name())
                .setGender(p.gender())
                // AVSC: ["null", {"type":"int","logicalType":"date"}] -> LocalDate (nullable)
                .setBirthDateEpochDays(p.birthDate())
                // AVSC: ["null", {"type":"string","logicalType":"uuid"}] -> UUID (nullable)
                .setSignupSessionId(p.signupSessionId())
                .build();
    }

    private EmailChangeCommittedV1 mapEmailChangeCommittedV1(OutboxEvent event) {
        EmailChangeCommittedPayload p = read(event.getPayload(), EmailChangeCommittedPayload.class);

        return EmailChangeCommittedV1.newBuilder()
                .setEventId(UUID.fromString(event.getId().toString()))
                .setOccurredAtEpochMillis(toInstantUtc(event.getOccurredAt()))
                .setUserId(p.userId())
                .setOldEmail(p.oldEmail())
                .setNewEmail(p.newEmail())
                .build();
    }

    private Instant toInstantUtc(java.time.LocalDateTime occurredAt) {
        return occurredAt.atZone(ZoneOffset.UTC).toInstant();
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return jsonMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse outbox payload into " + type.getSimpleName(), e);
        }
    }

    // payload JSON shapes you store in outbox (NOT Avro)
    public record AuthUserRegisteredPayload(
            long userId,
            String email,
            String phoneNumber,
            String name,
            String gender,
            LocalDate birthDate,
            UUID signupSessionId
    ) {}

    public record EmailChangeCommittedPayload(
            long userId,
            String oldEmail,
            String newEmail
    ) {}
}