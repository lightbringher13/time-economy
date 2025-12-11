package com.timeeconomy.auth_service.domain.exception;

public class EmailChangeEmailMismatchException extends RuntimeException {

    public EmailChangeEmailMismatchException() {
        super("Email mismatch during change-email commit");
    }
}