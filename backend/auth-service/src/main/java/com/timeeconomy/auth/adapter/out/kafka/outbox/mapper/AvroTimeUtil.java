package com.timeeconomy.auth.adapter.out.kafka.outbox.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class AvroTimeUtil {
    private AvroTimeUtil() {}

    public static Instant toInstantUtc(LocalDateTime dt) {
        return dt.atZone(ZoneOffset.UTC).toInstant();
    }
}