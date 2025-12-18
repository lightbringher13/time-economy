package com.timeeconomy.auth.domain.exception;

public class EmailVerificationExpiredException extends RuntimeException {
    public EmailVerificationExpiredException(String message) {
        super(message);
    }
}