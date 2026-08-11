package com.timeeconomy.notification.adapter.out.sms;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;
import com.timeeconomy.notification.config.LocalSmsInboxProperties;

@Component
@Profile("sms-inbox & sms-mock & (local | docker | test) & !prod")
public class InMemoryLocalSmsInboxAdapter implements LocalSmsInboxPort {

    private final Clock clock;
    private final int capacity;
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public InMemoryLocalSmsInboxAdapter(Clock clock, LocalSmsInboxProperties properties) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.capacity = Objects.requireNonNull(properties, "properties").capacity();
    }

    @Override
    public synchronized void store(Entry entry) {
        Objects.requireNonNull(entry, "entry");
        Instant now = clock.instant();
        purgeExpired(now);

        if (!entry.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Cannot store an expired local SMS message");
        }

        Entry existing = entries.get(entry.challengeId());
        if (existing != null) {
            if (sameDelivery(existing, entry)) {
                return;
            }
            throw new IllegalStateException("A different local SMS already exists for this challenge");
        }

        if (entries.size() >= capacity) {
            UUID oldest = entries.keySet().iterator().next();
            entries.remove(oldest);
        }

        entries.put(entry.challengeId(), entry);
    }

    @Override
    public synchronized List<Entry> findRecent(int limit) {
        purgeExpired(clock.instant());
        if (limit <= 0 || entries.isEmpty()) {
            return List.of();
        }

        List<Entry> recent = new ArrayList<>(entries.values());
        Collections.reverse(recent);
        return List.copyOf(recent.subList(0, Math.min(limit, recent.size())));
    }

    @Override
    public synchronized Optional<Entry> consume(UUID challengeId) {
        Objects.requireNonNull(challengeId, "challengeId");
        purgeExpired(clock.instant());
        return Optional.ofNullable(entries.remove(challengeId));
    }

    private void purgeExpired(Instant now) {
        entries.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
    }

    private static boolean sameDelivery(Entry existing, Entry incoming) {
        return existing.eventId().equals(incoming.eventId())
                && existing.challengeId().equals(incoming.challengeId())
                && existing.otp().equals(incoming.otp())
                && existing.maskedDestination().equals(incoming.maskedDestination())
                && existing.templateKey().equals(incoming.templateKey())
                && existing.purpose().equals(incoming.purpose())
                && existing.expiresAt().equals(incoming.expiresAt());
    }
}
