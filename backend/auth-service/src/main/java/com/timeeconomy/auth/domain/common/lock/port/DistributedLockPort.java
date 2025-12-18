package com.timeeconomy.auth.domain.common.lock.port;

/**
 * Abstraction for acquiring a distributed lock by key.
 * 
 * Typical usage:
 * 
 * try (LockHandle lock = distributedLockPort.acquireLock("change-email:user:" + userId)) {
 *     // critical section
 * }
 */
public interface DistributedLockPort {

    /**
     * Acquire a lock for the given key.
     * 
     * Implementations may block until the lock is acquired, 
     * or throw if acquisition fails (e.g. timeout).
     */
    LockHandle acquireLock(String key);
}