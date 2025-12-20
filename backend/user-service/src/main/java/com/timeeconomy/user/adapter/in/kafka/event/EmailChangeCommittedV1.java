package com.timeeconomy.user.adapter.in.kafka.event;


public record EmailChangeCommittedV1(
        String requestId,
        Long userId,
        String oldEmail,
        String newEmail,
        String occurredAt
) {}