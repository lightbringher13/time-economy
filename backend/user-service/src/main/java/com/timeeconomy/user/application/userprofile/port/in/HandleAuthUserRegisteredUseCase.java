package com.timeeconomy.user.application.userprofile.port.in;

import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;

public interface HandleAuthUserRegisteredUseCase {
    void handle(AuthUserRegisteredV1 event);
}