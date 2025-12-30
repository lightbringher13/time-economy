package com.timeeconomy.auth.adapter.in.worker;

import com.timeeconomy.auth.domain.outbox.port.in.RelayOutboxUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayWorker implements SmartLifecycle {

    private final RelayOutboxUseCase relayOutboxUseCase;
    private final Clock clock; // Clock.systemUTC() bean recommended

    private final AtomicBoolean running = new AtomicBoolean(false);

    // BigCo style: scheduler owns the loop, worker does "one tick"
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduled;

    private final String workerId = "auth-outbox-relay-" + UUID.randomUUID();

    // tuneable
    private final int batchSize = 50;
    private final Duration lease = Duration.ofSeconds(10);
    private final Duration idleDelay = Duration.ofMillis(250);
    private final Duration errorDelay = Duration.ofMillis(1000);

    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
    @Override public boolean isRunning() { return running.get(); }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) return;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "outbox-relay-worker");
            t.setDaemon(true);
            return t;
        });

        // ✅ No cancel/reschedule churn:
        // run at a small fixed rate, but inside we back off using a "nextRunAt" gate.
        // (This is a common pattern in production schedulers.)
        this.nextRunAtMillis = 0L;
        this.scheduled = scheduler.scheduleAtFixedRate(this::tickGateSafe, 0, 50, TimeUnit.MILLISECONDS);

        log.info("[OUTBOX] relay worker started. workerId={}, batchSize={}, lease={}",
                workerId, batchSize, lease);
    }

    // -----------------------
    // Gate-based dynamic delay
    // -----------------------
    private volatile long nextRunAtMillis = 0L;

    private void tickGateSafe() {
        if (!running.get()) return;

        long nowMs = Instant.now(clock).toEpochMilli();
        if (nowMs < nextRunAtMillis) {
            return; // skip until it's time (dynamic delay)
        }

        try {
            var result = relayOutboxUseCase.relayOnce(
                    new RelayOutboxUseCase.RelayCommand(
                            workerId,
                            batchSize,
                            lease,
                            Instant.ofEpochMilli(nowMs) // consistent "now" for this tick
                    )
            );

            // if no work, back off; else run ASAP (no delay)
            nextRunAtMillis = nowMs + (result.claimed() == 0 ? idleDelay.toMillis() : 0L);

        } catch (Throwable t) {
            log.error("[OUTBOX] relay tick error", t);
            nextRunAtMillis = nowMs + errorDelay.toMillis();
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) return;

        try {
            if (scheduled != null) scheduled.cancel(true);
        } catch (Exception ignored) {}

        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("[OUTBOX] relay worker stopped. workerId={}", workerId);
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}