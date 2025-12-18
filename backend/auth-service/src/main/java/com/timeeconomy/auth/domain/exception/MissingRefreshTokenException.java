package com.timeeconomy.auth.domain.exception;

public class MissingRefreshTokenException extends RuntimeException {
    public MissingRefreshTokenException() {
        super("Refresh token is missing");
    }
}