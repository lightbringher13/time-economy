package com.timeeconomy.auth.domain.changeemail.exception;

public class InvalidSecondFactorCodeException extends RuntimeException {
    public InvalidSecondFactorCodeException() {
        super("Invalid second factor code or invalid request state");
    }
}