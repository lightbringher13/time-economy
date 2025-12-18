package com.timeeconomy.auth.domain.exception;

public class AuthUserNotFoundException extends RuntimeException {

    public AuthUserNotFoundException(Long userId) {
        super("Auth user not found. userId=" + userId);
    }

    public AuthUserNotFoundException(String message) {
        super(message);
    }
}