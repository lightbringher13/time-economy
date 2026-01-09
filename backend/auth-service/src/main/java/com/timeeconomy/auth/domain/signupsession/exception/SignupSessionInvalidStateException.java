package com.timeeconomy.auth.domain.signupsession.exception;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public class SignupSessionInvalidStateException extends RuntimeException {
    public SignupSessionInvalidStateException(String action, SignupSessionState state) {
        super("Invalid signup session state for " + action + ": " + state);
    }
}