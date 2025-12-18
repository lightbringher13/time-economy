package com.timeeconomy.user.domain.exception;

public class InvalidUserProfileException extends RuntimeException {
    public InvalidUserProfileException(String message) {
        super(message);
    }
}