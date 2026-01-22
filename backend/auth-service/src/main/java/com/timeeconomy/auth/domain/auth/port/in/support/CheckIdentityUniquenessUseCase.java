// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/auth/port/in/CheckIdentityUniquenessUseCase.java
package com.timeeconomy.auth.domain.auth.port.in.support;

public interface CheckIdentityUniquenessUseCase {

    enum Outcome {
        AVAILABLE,
        EMAIL_ALREADY_USED,
        PHONE_ALREADY_USED,
        BOTH_ALREADY_USED,
        INVALID_INPUT
    }

    record Query(
            String email,       // normalized email expected (trim/lowercase) or null
            String phoneNumber  // normalized phone expected (trim/E.164 later) or null
    ) {}

    record Result(
            Outcome outcome,
            boolean emailAvailable,
            boolean phoneAvailable
    ) {}

    Result check(Query query);
}