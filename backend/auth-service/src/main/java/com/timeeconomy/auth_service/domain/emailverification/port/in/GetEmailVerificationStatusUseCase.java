package com.timeeconomy.auth_service.domain.emailverification.port.in;

public interface GetEmailVerificationStatusUseCase {

    record StatusResult(boolean verified) {}

    StatusResult getStatus(String email);
}