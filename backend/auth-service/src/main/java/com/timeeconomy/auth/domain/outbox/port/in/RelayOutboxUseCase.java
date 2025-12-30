package com.timeeconomy.auth.domain.outbox.port.in;

import java.time.Duration;
import java.time.Instant;

public interface RelayOutboxUseCase {

    RelayResult relayOnce(RelayCommand command);

    record RelayCommand(
            String workerId,
            int limit,
            Duration lease,
            Instant now
    ) {
        public long leaseSeconds() {
            return lease == null ? 0L : lease.getSeconds();
        }
    }

    record RelayResult(
            int claimed,
            int sent,
            int failed
    ) {
        public boolean hasWork() { return claimed > 0; }
    }
}