package com.timeeconomy.user.application.userprofile.port.in;

import com.timeeconomy.user.adapter.in.kafka.event.AuthUserRegisteredV1;

public interface HandleAuthUserRegisteredUseCase {
    void handle(AuthUserRegisteredV1 event);
}