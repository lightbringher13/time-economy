package com.timeeconomy.auth_service.domain.exception;

public class SessionAlreadyRevokedException extends RuntimeException {
    public SessionAlreadyRevokedException() {
        super("Session already revoked");
    }
}
