package com.timeeconomy.notification.application.integration.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.timeeconomy.notification.application.integration.port.out.AuthInternalLinkClientPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkDeliveryAcknowledgmentCoordinator {

    private final AuthInternalLinkClientPort authInternalLinkClientPort;

    public void acknowledgeAfterCommit(UUID eventId, UUID challengeId) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(challengeId, "challengeId");

        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Link acknowledgment requires an active transaction");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    authInternalLinkClientPort.acknowledgeLink(challengeId);
                } catch (Exception ex) {
                    // Auth keeps the raw link token under its original TTL as the recovery backstop.
                    // The provider result and DB transaction are already committed, so do not retry delivery.
                    log.warn(
                            "[LINK_ACK][DEFERRED_TO_TTL] acknowledgment failed eventId={} challengeId={} cause={}",
                            eventId,
                            challengeId,
                            ex.getClass().getSimpleName()
                    );
                }
            }
        });
    }
}
