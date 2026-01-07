// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/changeemail/exception/EmailChangeForbiddenException.java
package com.timeeconomy.auth.domain.changeemail.exception;

public class EmailChangeForbiddenException extends RuntimeException {
    public EmailChangeForbiddenException() {
        super("Forbidden");
    }
}