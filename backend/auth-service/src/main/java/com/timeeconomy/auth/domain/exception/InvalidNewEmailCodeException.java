package com.timeeconomy.auth.domain.exception;

public class InvalidNewEmailCodeException extends RuntimeException {

    public InvalidNewEmailCodeException() {
        super("Invalid or expired new email verification code");
    }
}