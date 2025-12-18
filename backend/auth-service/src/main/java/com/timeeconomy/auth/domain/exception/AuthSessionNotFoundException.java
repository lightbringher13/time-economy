package com.timeeconomy.auth.domain.exception;

public class AuthSessionNotFoundException extends RuntimeException {
    public AuthSessionNotFoundException() {
        super("Auth session not found");
    }
}
