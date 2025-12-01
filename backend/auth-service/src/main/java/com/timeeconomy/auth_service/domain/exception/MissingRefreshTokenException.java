package com.timeeconomy.auth_service.domain.exception;

public class MissingRefreshTokenException extends RuntimeException {
    public MissingRefreshTokenException() {
        super("Refresh token is missing");
    }
}