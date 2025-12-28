package com.timeeconomy.notification.application.notification.exception;

public class EmailSendFailedException extends RuntimeException {
    public EmailSendFailedException(String message) { super(message); }
    public EmailSendFailedException(String message, Throwable cause) { super(message, cause); }
}