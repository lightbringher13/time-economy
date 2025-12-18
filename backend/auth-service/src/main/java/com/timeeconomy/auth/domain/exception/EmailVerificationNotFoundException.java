package com.timeeconomy.auth.domain.exception;

public class EmailVerificationNotFoundException extends RuntimeException {
    public EmailVerificationNotFoundException(String message) {
        super(message);
    }
}