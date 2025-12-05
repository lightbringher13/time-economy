package com.timeeconomy.auth_service.domain.port.in;

public interface GetEmailVerificationStatusUseCase {

    record StatusResult(boolean verified) {}

    StatusResult getStatus(String email);
}