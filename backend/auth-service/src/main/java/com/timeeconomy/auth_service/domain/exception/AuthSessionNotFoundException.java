package com.timeeconomy.auth_service.domain.exception;

public class AuthSessionNotFoundException extends RuntimeException {
    public AuthSessionNotFoundException() {
        super("Auth session not found");
    }
}
