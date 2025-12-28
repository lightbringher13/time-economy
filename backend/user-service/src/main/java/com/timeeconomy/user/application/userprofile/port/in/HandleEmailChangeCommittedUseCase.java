package com.timeeconomy.user.application.userprofile.port.in;

import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;

public interface HandleEmailChangeCommittedUseCase {
    void handle(EmailChangeCommittedV1 event);
}