// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/exception/SignupSessionNotActiveException.java
package com.timeeconomy.auth.domain.signupsession.exception;

public class SignupSessionNotActiveException extends RuntimeException {
    public SignupSessionNotActiveException(String message) {
        super(message);
    }
}