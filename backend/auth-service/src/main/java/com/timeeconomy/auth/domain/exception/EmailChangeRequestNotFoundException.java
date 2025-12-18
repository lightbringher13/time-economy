package com.timeeconomy.auth.domain.exception;

public class EmailChangeRequestNotFoundException extends RuntimeException {

    public EmailChangeRequestNotFoundException(Long userId, Long requestId) {
        super("Email change request not found. userId=" + userId + ", requestId=" + requestId);
    }
}