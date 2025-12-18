package com.timeeconomy.auth.domain.exception;

import java.util.UUID;

public class SignupSessionNotFoundException extends RuntimeException {

    public SignupSessionNotFoundException(UUID sessionId) {
        super("Signup session not found or expired: " + sessionId);
    }

    public SignupSessionNotFoundException(String message) {
        super(message);
    }
}