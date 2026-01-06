package com.timeeconomy.auth.domain.changeemail.exception;

public class EmailChangeStateCorruptedException extends RuntimeException {
    private final Long requestId;
    private final Long userId;

    public EmailChangeStateCorruptedException(Long requestId, Long userId, String reason) {
        super(reason);
        this.requestId = requestId;
        this.userId = userId;
    }

    public Long getRequestId() { return requestId; }
    public Long getUserId() { return userId; }
}