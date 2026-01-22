// backend/auth-service/src/main/java/com/timeeconomy/auth/adapter/out/signupsession/integration/SignupIdentityUniquenessAdapter.java
package com.timeeconomy.auth.adapter.out.signupsession.integration;

import com.timeeconomy.auth.domain.auth.port.in.support.CheckIdentityUniquenessUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupIdentityUniquenessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupIdentityUniquenessAdapter implements SignupIdentityUniquenessPort {

    private final CheckIdentityUniquenessUseCase checkIdentityUniquenessUseCase;

    @Override
    public Result checkEmail(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return new Result(Outcome.INVALID_INPUT);
        }

        var r = checkIdentityUniquenessUseCase.check(
                new CheckIdentityUniquenessUseCase.Query(normalizedEmail, null)
        );

        return switch (r.outcome()) {
            case AVAILABLE -> new Result(Outcome.AVAILABLE);
            case EMAIL_ALREADY_USED, BOTH_ALREADY_USED -> new Result(Outcome.EMAIL_ALREADY_USED);
            case PHONE_ALREADY_USED -> new Result(Outcome.AVAILABLE); // irrelevant for email-only query
            case INVALID_INPUT -> new Result(Outcome.INVALID_INPUT);
        };
    }

    @Override
    public Result checkPhone(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return new Result(Outcome.INVALID_INPUT);
        }

        var r = checkIdentityUniquenessUseCase.check(
                new CheckIdentityUniquenessUseCase.Query(null, normalizedPhone)
        );

        return switch (r.outcome()) {
            case AVAILABLE -> new Result(Outcome.AVAILABLE);
            case PHONE_ALREADY_USED, BOTH_ALREADY_USED -> new Result(Outcome.PHONE_ALREADY_USED);
            case EMAIL_ALREADY_USED -> new Result(Outcome.AVAILABLE); // irrelevant for phone-only query
            case INVALID_INPUT -> new Result(Outcome.INVALID_INPUT);
        };
    }
}