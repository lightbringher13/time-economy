// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/changeemail/exception/EmailChangeInvalidStateException.java
package com.timeeconomy.auth.domain.changeemail.exception;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public class EmailChangeInvalidStateException extends RuntimeException {
    public EmailChangeInvalidStateException(String action, EmailChangeStatus status) {
        super("Invalid state for " + action + ": status=" + status);
    }
}