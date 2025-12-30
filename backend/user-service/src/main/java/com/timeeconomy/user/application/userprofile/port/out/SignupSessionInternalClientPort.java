package com.timeeconomy.user.application.userprofile.port.out;

import com.timeeconomy.user.adapter.out.authclient.dto.CompletedSignupSessionResponse;

import java.util.UUID;

public interface SignupSessionInternalClientPort {
    CompletedSignupSessionResponse getCompletedSession(UUID signupSessionId);
}