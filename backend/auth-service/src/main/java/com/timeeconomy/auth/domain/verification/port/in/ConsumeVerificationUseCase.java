package com.timeeconomy.auth.domain.verification.port.in;

public interface ConsumeVerificationUseCase {

    void consume(ConsumeCommand command);

    record ConsumeCommand(String challengeId) {}
}