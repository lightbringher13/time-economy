// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/exception/SignupSessionNotCompletedException.java
package com.timeeconomy.auth.domain.signupsession.exception;

public class SignupSessionNotCompletedException extends RuntimeException {
    public SignupSessionNotCompletedException(String message) { super(message); }
}