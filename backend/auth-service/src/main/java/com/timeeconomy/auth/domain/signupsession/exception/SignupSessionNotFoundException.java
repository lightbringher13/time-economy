// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/exception/SignupSessionNotFoundException.java
package com.timeeconomy.auth.domain.signupsession.exception;

public class SignupSessionNotFoundException extends RuntimeException {
    public SignupSessionNotFoundException(String message) { super(message); }
}