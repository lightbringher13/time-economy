package com.timeeconomy.auth.domain.exception;

public class PhoneNumberAlreadyUsedException extends RuntimeException {
    public PhoneNumberAlreadyUsedException(String message) {
        super(message);
    }
}