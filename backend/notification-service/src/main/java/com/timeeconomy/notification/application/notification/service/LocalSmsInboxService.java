package com.timeeconomy.notification.application.notification.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.timeeconomy.notification.application.notification.port.in.LocalSmsInboxUseCase;
import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;

import lombok.RequiredArgsConstructor;

@Service
@Profile("sms-inbox & sms-mock & (local | docker | test) & !prod")
@RequiredArgsConstructor
public class LocalSmsInboxService implements LocalSmsInboxUseCase {

    private final LocalSmsInboxPort inbox;
    private final Clock clock;

    @Override
    public List<MessageSummary> findRecent(int limit) {
        return inbox.findRecent(limit).stream()
                .map(entry -> new MessageSummary(
                        entry.eventId(),
                        entry.challengeId(),
                        entry.maskedDestination(),
                        entry.templateKey(),
                        entry.purpose(),
                        entry.createdAt(),
                        entry.expiresAt()
                ))
                .toList();
    }

    @Override
    public Optional<ConsumedMessage> consume(UUID challengeId) {
        if (challengeId == null) {
            return Optional.empty();
        }

        return inbox.consume(challengeId)
                .filter(entry -> entry.expiresAt().isAfter(clock.instant()))
                .map(entry -> new ConsumedMessage(
                        entry.challengeId(),
                        entry.otp(),
                        entry.expiresAt()
                ));
    }
}
