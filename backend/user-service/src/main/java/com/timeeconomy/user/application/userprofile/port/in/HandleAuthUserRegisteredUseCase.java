package com.timeeconomy.user.application.userprofile.port.in;

import com.timeeconomy.contracts.auth.v2.AuthUserRegisteredV2;

public interface HandleAuthUserRegisteredUseCase {
    void handle(AuthUserRegisteredV2 event);
}