package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
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
                .map(session -> mapToResult(session))
                .orElseGet(() -> new Result(
                        false,
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        null,
                        SessionState.EXPIRED_OR_NOT_FOUND
                ));
    }

    private Result mapToResult(SignupSession s) {
        GetSignupSessionStatusUseCase.SessionState state;

        switch (s.getState()) { // assuming you have SignupSessionState enum
            case EMAIL_PENDING -> state = SessionState.EMAIL_PENDING;
            case EMAIL_VERIFIED -> state = SessionState.EMAIL_VERIFIED;
            case PROFILE_FILLED -> state = SessionState.PROFILE_FILLED;
            case COMPLETED -> state = SessionState.COMPLETED;
            default -> state = SessionState.EXPIRED_OR_NOT_FOUND;
        }

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