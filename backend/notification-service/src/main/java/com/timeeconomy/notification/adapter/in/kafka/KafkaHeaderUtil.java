package com.timeeconomy.notification.adapter.in.kafka;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class KafkaHeaderUtil {
    private KafkaHeaderUtil() {}

    public static String str(Headers headers, String key) {
        Header h = headers.lastHeader(key);
        if (h == null || h.value() == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }

    public static UUID uuid(Headers headers, String key) {
        String v = str(headers, key);
        return (v == null) ? null : UUID.fromString(v);
    }
}