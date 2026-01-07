// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/changeemail/exception/EmailChangeRequestNotFoundException.java
package com.timeeconomy.auth.domain.changeemail.exception;

public class EmailChangeRequestNotFoundException extends RuntimeException {
    public EmailChangeRequestNotFoundException(Long requestId) {
        super("Email change request not found: requestId=" + requestId);
    }
}