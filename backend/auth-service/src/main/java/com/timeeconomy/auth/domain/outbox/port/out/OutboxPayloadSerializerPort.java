package com.timeeconomy.auth.domain.outbox.port.out;

public interface OutboxPayloadSerializerPort {
    String serialize(Object payload);
}