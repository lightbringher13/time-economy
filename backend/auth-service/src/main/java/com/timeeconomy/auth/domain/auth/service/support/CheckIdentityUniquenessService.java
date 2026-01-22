// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/auth/service/CheckIdentityUniquenessService.java
package com.timeeconomy.auth.domain.auth.service.support;

import com.timeeconomy.auth.domain.auth.port.in.support.CheckIdentityUniquenessUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckIdentityUniquenessService implements CheckIdentityUniquenessUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;

    @Override
    public Result check(Query q) {
        if (q == null) {
            return new Result(Outcome.INVALID_INPUT, false, false);
        }

        String email = normalizeEmail(q.email());
        String phone = normalizePhone(q.phoneNumber());

        // If neither is provided, treat as invalid input (your policy can differ)
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            return new Result(Outcome.INVALID_INPUT, false, false);
        }

        boolean emailTaken = false;
        boolean phoneTaken = false;

        if (email != null && !email.isBlank()) {
            emailTaken = authUserRepositoryPort.findByEmail(email).isPresent();
        }

        if (phone != null && !phone.isBlank()) {
            phoneTaken = authUserRepositoryPort.findByPhoneNumber(phone).isPresent();
        }

        if (emailTaken && phoneTaken) {
            return new Result(Outcome.BOTH_ALREADY_USED, false, false);
        }

        if (emailTaken) {
            return new Result(Outcome.EMAIL_ALREADY_USED, false, true);
        }

        if (phoneTaken) {
            return new Result(Outcome.PHONE_ALREADY_USED, true, false);
        }

        return new Result(Outcome.AVAILABLE, true, true);
    }

    // -------------------------
    // normalization helpers
    // -------------------------

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        return v.isBlank() ? null : v;
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isBlank() ? null : v;
        // TODO: E.164 normalize later if you want
    }
}