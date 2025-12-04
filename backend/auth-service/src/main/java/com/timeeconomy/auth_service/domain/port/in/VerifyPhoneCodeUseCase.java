package com.timeeconomy.auth_service.domain.port.in;

public interface VerifyPhoneCodeUseCase {

    Result verify(VerifyCommand command);

    record VerifyCommand(
            String phoneNumber,
            String code) {
    }

    record Result(
            boolean success) {
    }
}