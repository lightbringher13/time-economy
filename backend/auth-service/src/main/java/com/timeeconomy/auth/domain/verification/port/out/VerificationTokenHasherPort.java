package com.timeeconomy.auth.domain.verification.port.out;

public interface VerificationTokenHasherPort {
    String hash(String rawToken);
}