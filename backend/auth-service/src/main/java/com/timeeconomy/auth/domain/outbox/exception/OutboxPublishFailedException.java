package com.timeeconomy.auth.domain.outbox.exception;

public class OutboxPublishFailedException extends RuntimeException {
    public OutboxPublishFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}