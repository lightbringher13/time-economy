package com.timeeconomy.notification.application.integration.port.out;

import com.timeeconomy.notification.adapter.out.authclient.dto.CompletedSignupSessionResponse;

import java.util.UUID;

public interface SignupSessionInternalClientPort {
    CompletedSignupSessionResponse getCompletedSession(UUID signupSessionId);
}