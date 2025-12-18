package com.timeeconomy.auth_service.domain.common.lock.port;

/**
 * A handle to a distributed lock.
 * Implementations should release the lock when {@link #close()} is called.
 */
public interface LockHandle extends AutoCloseable {

    @Override
    void close();
}