package com.timeeconomy.auth.domain.outbox.model;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}