package com.timeeconomy.auth_service.domain.port.in;

public interface SendEmailVerificationCodeUseCase {

    record SendCommand(
            String email
    ) {}

    // dev 환경에서는 code 반환, prod에서는 빈 구조로 바꿔도 됨
    record SendResult(
            String code
    ) {}

    SendResult send(SendCommand command);
}