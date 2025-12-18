package com.timeeconomy.user.domain.exception;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(Long userId) {
        super("UserProfile not found: " + userId);
    }
}