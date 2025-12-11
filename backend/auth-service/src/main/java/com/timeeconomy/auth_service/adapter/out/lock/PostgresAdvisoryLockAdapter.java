package com.timeeconomy.auth_service.adapter.out.lock;

import com.timeeconomy.auth_service.domain.port.out.DistributedLockPort;
import com.timeeconomy.auth_service.domain.port.out.LockHandle;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DistributedLockPort implementation using PostgreSQL advisory locks.
 *
 * This provides a real distributed lock across all instances of the service
 * that share the same Postgres database.
 */
@Component
@RequiredArgsConstructor
public class PostgresAdvisoryLockAdapter implements DistributedLockPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public LockHandle acquireLock(String key) {
        long lockKey = toLockKey(key);

        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_lock(?)",
                Boolean.class,
                lockKey
        );

        if (Boolean.FALSE.equals(acquired)) {
            // You can define your own specific exception type here later
            throw new RuntimeException("Could not acquire advisory lock for key: " + key);
        }

        return new PostgresAdvisoryLockHandle(lockKey, jdbcTemplate);
    }

    /**
     * Map a String key to a 64-bit lock key.
     * For now we use hashCode; you can replace with a better hash if needed.
     */
    private long toLockKey(String key) {
        // Simple and stable for now; later you can use a stronger 64-bit hash
        return (long) key.hashCode();
    }

    private static class PostgresAdvisoryLockHandle implements LockHandle {

        private final long lockKey;
        private final JdbcTemplate jdbcTemplate;
        private boolean released = false;

        private PostgresAdvisoryLockHandle(long lockKey, JdbcTemplate jdbcTemplate) {
            this.lockKey = lockKey;
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public void close() {
            if (released) {
                return;
            }
            jdbcTemplate.queryForObject(
                    "SELECT pg_advisory_unlock(?)",
                    Boolean.class,
                    lockKey
            );
            released = true;
        }
    }
}