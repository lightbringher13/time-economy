package com.timeeconomy.auth_service.domain.verification.port.out;

public interface VerificationTokenHasherPort {
    String hash(String rawToken);
}