package com.timeeconomy.auth_service.domain.exception;

public class InvalidSecondFactorCodeException extends RuntimeException {

    public InvalidSecondFactorCodeException() {
        super("Invalid or expired second factor code");
    }
}