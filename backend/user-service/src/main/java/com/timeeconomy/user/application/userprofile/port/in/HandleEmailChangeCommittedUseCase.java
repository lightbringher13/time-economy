package com.timeeconomy.user.application.userprofile.port.in;

import com.timeeconomy.user.adapter.in.kafka.event.EmailChangeCommittedV1;

public interface HandleEmailChangeCommittedUseCase {
    void handle(EmailChangeCommittedV1 event);
}