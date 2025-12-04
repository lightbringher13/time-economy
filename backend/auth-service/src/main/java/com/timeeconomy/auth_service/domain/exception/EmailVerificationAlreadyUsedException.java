package com.timeeconomy.auth_service.domain.exception;

public class EmailVerificationAlreadyUsedException extends RuntimeException {
    public EmailVerificationAlreadyUsedException(String message) {
        super(message);
    }
}