package com.timeeconomy.auth_service.domain.port.in;

public interface VerifyEmailCodeUseCase {

    VerifyResult verify(VerifyCommand command);

    record VerifyCommand(
            String email,
            String code) {
    }

    record VerifyResult(
            boolean success) {
    }
}