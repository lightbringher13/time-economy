package com.timeeconomy.auth_service.domain.signupsession.service;

import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth_service.domain.signupsession.port.in.GetSignupSessionStatusUseCase;
import com.timeeconomy.auth_service.domain.signupsession.port.out.SignupSessionRepositoryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSignupSessionStatusService implements GetSignupSessionStatusUseCase {

    private final SignupSessionRepositoryPort signupSessionRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public Result getStatus(Query query) {
        UUID sessionId = query.sessionId();
        LocalDateTime now = LocalDateTime.now();

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