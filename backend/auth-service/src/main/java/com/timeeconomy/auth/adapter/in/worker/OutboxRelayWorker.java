package com.timeeconomy.auth.adapter.in.worker;

import com.timeeconomy.auth.domain.outbox.port.in.RelayOutboxUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayWorker implements SmartLifecycle {

    private final RelayOutboxUseCase relayOutboxUseCase;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    private final String workerId = "auth-outbox-relay-" + UUID.randomUUID();
    private final int batchSize = 50;
    private final Duration lease = Duration.ofSeconds(10);
    private final long idleSleepMs = 250;
    private final long errorSleepMs = 1000;

    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) return;

        workerThread = new Thread(this::runLoop, "outbox-relay-worker");
        workerThread.setDaemon(true);
        workerThread.start();

        log.info("[OUTBOX] relay worker started. workerId={}", workerId);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(java.time.Clock.systemUTC());
    }

    private void runLoop() {
        while (running.get()) {
            try {
                var result = relayOutboxUseCase.relayOnce(
                        new RelayOutboxUseCase.RelayCommand(workerId, batchSize, lease, nowUtc())
                );

                if (result.claimed() == 0) {
                    Thread.sleep(idleSleepMs);
                } else {
                    Thread.onSpinWait(); // optional
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                log.error("[OUTBOX] relay loop error", e);
                try {
                    Thread.sleep(errorSleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("[OUTBOX] relay worker stopped. workerId={}", workerId);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) return;

        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}