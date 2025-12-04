package com.timeeconomy.auth_service.domain.exception;

public class EmailVerificationNotFoundException extends RuntimeException {
    public EmailVerificationNotFoundException(String message) {
        super(message);
    }
}