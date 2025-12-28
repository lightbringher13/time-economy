package com.timeeconomy.auth.domain.outbox.port.in;

import java.time.Duration;
import java.time.LocalDateTime;

public interface RelayOutboxUseCase {

    RelayResult relayOnce(RelayCommand command);

    record RelayCommand(
            String workerId,
            int limit,
            Duration lease,
            LocalDateTime now
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