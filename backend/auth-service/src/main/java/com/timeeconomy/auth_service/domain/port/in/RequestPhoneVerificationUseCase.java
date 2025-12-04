package com.timeeconomy.auth_service.domain.port.in;

public interface RequestPhoneVerificationUseCase {

    void requestVerification(RequestCommand command);

    record RequestCommand(
            String phoneNumber,
            String countryCode // nullable → default handled in impl
    ) {
    }
}