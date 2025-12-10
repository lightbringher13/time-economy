package com.timeeconomy.auth_service.domain.exception;

public class PhoneNumberAlreadyUsedException extends RuntimeException {
    public PhoneNumberAlreadyUsedException(String message) {
        super(message);
    }
}