package com.timeeconomy.auth.domain.exception;

public class EmailVerificationAlreadyUsedException extends RuntimeException {
    public EmailVerificationAlreadyUsedException(String message) {
        super(message);
    }
}