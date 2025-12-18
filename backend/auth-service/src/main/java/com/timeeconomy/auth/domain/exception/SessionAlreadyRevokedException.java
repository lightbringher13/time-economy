package com.timeeconomy.auth.domain.exception;

public class SessionAlreadyRevokedException extends RuntimeException {
    public SessionAlreadyRevokedException() {
        super("Session already revoked");
    }
}
