// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/service/GetSignupSessionStatusService.java
package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.GetSignupSessionStatusUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.persistence.SignupSessionStorePort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSignupSessionStatusService implements GetSignupSessionStatusUseCase {

    private final SignupSessionStorePort signupSessionRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Result getStatus(Query query) {
        UUID sessionId = (query == null) ? null : query.sessionId();

        if (sessionId == null) {
            return noSession();
        }

        Instant now = Instant.now(clock);

        return signupSessionRepositoryPort.findActiveById(sessionId, now)
                .map(this::mapToResult)
                .orElseGet(this::noSession);
    }

    private Result noSession() {
        return new Result(
                false,

                null,
                false,
                false,
                null,          // ✅ emailOtpExpiresAt

                null,
                false,
                false,
                null,          // ✅ phoneOtpExpiresAt

                null,
                null,
                null,
                SignupSessionState.DRAFT
        );
    }

    private Result mapToResult(SignupSession s) {
        SignupSessionState state =
                (s.getState() == null) ? SignupSessionState.DRAFT : s.getState();

        return new Result(
                true,

                s.getEmail(),
                s.isEmailVerified(),
                s.isEmailOtpPending(),
                s.getEmailOtpExpiresAt(),   // ✅ NEW

                s.getPhoneNumber(),
                s.isPhoneVerified(),
                s.isPhoneOtpPending(),
                s.getPhoneOtpExpiresAt(),   // ✅ NEW

                s.getName(),
                s.getGender(),
                s.getBirthDate(),
                state
        );
    }
}