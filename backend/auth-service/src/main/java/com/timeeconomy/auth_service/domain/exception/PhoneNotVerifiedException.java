package com.timeeconomy.auth_service.domain.exception;

public class PhoneNotVerifiedException extends RuntimeException {

    public PhoneNotVerifiedException(String message) {
        super(message);
    }
}