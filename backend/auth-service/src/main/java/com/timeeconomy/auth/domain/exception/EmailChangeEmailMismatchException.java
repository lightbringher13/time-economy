package com.timeeconomy.auth.domain.exception;

public class EmailChangeEmailMismatchException extends RuntimeException {

    public EmailChangeEmailMismatchException() {
        super("Email mismatch during change-email commit");
    }
}