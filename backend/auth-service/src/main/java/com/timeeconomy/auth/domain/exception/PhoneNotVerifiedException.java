package com.timeeconomy.auth.domain.exception;

public class PhoneNotVerifiedException extends RuntimeException {

    public PhoneNotVerifiedException(String message) {
        super(message);
    }
}