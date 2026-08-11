package com.timeeconomy.auth.domain.verification.port.in.internal;

public interface AcknowledgeVerificationOtpDeliveryUseCase {

    record Command(String challengeId) {}

    void acknowledge(Command command);
}
