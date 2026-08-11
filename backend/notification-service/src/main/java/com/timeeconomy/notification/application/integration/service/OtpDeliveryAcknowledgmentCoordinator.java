package com.timeeconomy.notification.application.integration.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.timeeconomy.notification.application.integration.port.out.AuthInternalOtpClientPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpDeliveryAcknowledgmentCoordinator {

    private final AuthInternalOtpClientPort authInternalOtpClientPort;

    public void acknowledgeAfterCommit(UUID eventId, UUID challengeId) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(challengeId, "challengeId");

        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("OTP acknowledgment requires an active transaction");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    authInternalOtpClientPort.acknowledgeOtp(challengeId);
                } catch (Exception ex) {
                    // Auth keeps the raw OTP under its original TTL, which is the recovery backstop.
                    // Do not fail Kafka processing after the provider result and DB transaction committed.
                    log.warn(
                            "[OTP_ACK][DEFERRED_TO_TTL] acknowledgment failed eventId={} challengeId={} cause={}",
                            eventId,
                            challengeId,
                            ex.getClass().getSimpleName()
                    );
                }
            }
        });
    }
}
