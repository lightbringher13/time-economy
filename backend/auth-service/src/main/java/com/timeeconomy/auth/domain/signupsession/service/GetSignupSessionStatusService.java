// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/service/GetSignupSessionStatusService.java
package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.GetSignupSessionStatusUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

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
        UUID sessionId = query.sessionId();
        Instant now = Instant.now(clock);

        return signupSessionRepositoryPort.findActiveById(sessionId, now)
                .map(this::mapToResult)
                .orElseGet(() -> new Result(
                        false,
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        null,
                        SignupSessionState.EXPIRED // "not found / expired" fallback
                ));
    }

    private Result mapToResult(SignupSession s) {
        // Optional: if your store might still return terminal states, normalize here
        SignupSessionState state = s.getState() == null ? SignupSessionState.EXPIRED : s.getState();

        return new Result(
                true,
                s.getEmail(),
                s.isEmailVerified(),
                s.getPhoneNumber(),
                s.isPhoneVerified(),
                s.getName(),
                s.getGender(),
                s.getBirthDate(),
                state
        );
    }
}