package com.timeeconomy.auth.domain.outbox.service;

import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.in.RelayOutboxUseCase;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventPublisherPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelayOutboxService implements RelayOutboxUseCase {

    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final OutboxEventPublisherPort outboxEventPublisherPort;

    @Override
    public RelayResult relayOnce(RelayCommand command) {

        List<OutboxEvent> batch = outboxEventRepositoryPort.claimBatch(
                command.workerId(),
                command.limit(),
                command.lease(),
                command.now()
        );

        int claimed = batch.size();
        int sent = 0;
        int failed = 0;

        for (OutboxEvent event : batch) {
            int attempts = safeAttempts(event); // already incremented in claimBatch
            String eventId = event.getId().toString();
            String workerId = command.workerId();

            try {
                outboxEventPublisherPort.publish(event);

            } catch (Exception publishEx) {
                String error = abbreviate(publishEx.getMessage(), 500);

                try {
                    int updated = outboxEventRepositoryPort.markFailed(
                            event.getId(), workerId, attempts, error, command.now()
                    );

                    if (updated == 0) {
                        log.warn("[OUTBOX] publish failed but markFailed skipped (lost ownership). workerId={}, id={}, type={}, attempts={}, err={}",
                                workerId, eventId, event.getEventType(), attempts, error);
                    } else {
                        failed++;
                        log.warn("[OUTBOX] publish failed. workerId={}, id={}, type={}, attempts={}, err={}",
                                workerId, eventId, event.getEventType(), attempts, error);
                    }

                } catch (Exception markFailedEx) {
                    log.error("[OUTBOX] markFailed failed after publish failure. workerId={}, id={}, type={}, attempts={}, publishErr={}, markFailedErr={}",
                            workerId, eventId, event.getEventType(), attempts, error, markFailedEx.toString(), markFailedEx);
                }
                continue;
            }

            // publish succeeded
            try {
                int updated = outboxEventRepositoryPort.markSent(
                        event.getId(), workerId, command.now(), command.now()
                );

                if (updated == 0) {
                    log.warn("[OUTBOX] publish succeeded but markSent skipped (lost ownership). workerId={}, id={}, type={}, attempts={}",
                            workerId, eventId, event.getEventType(), attempts);
                } else {
                    sent++;
                }

            } catch (Exception markSentEx) {
                log.error("[OUTBOX] publish succeeded but markSent failed. workerId={}, id={}, type={}, attempts={}, err={}",
                        workerId, eventId, event.getEventType(), attempts, markSentEx.toString(), markSentEx);
            }
        }

        return new RelayResult(claimed, sent, failed);
    }

    private int safeAttempts(OutboxEvent event) {
        try {
            return event.getAttempts();
        } catch (Exception ignore) {
            return 0;
        }
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}