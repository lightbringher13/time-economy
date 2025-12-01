package com.timeeconomy.user_service.domain.exception;

public class InvalidUserProfileException extends RuntimeException {
    public InvalidUserProfileException(String message) {
        super(message);
    }
}