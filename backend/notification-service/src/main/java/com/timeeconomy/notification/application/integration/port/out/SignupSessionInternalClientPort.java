package com.timeeconomy.notification.application.integration.port.out;

import java.util.UUID;

import com.timeeconomy.notification.adapter.out.authclient.dto.response.CompletedSignupSessionResponse;

public interface SignupSessionInternalClientPort {
    CompletedSignupSessionResponse getCompletedSession(UUID signupSessionId);
}