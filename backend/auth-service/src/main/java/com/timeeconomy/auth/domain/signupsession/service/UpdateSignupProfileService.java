package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSignupProfileService implements UpdateSignupProfileUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final Clock clock;

    @Override
    @Transactional
    public Result updateProfile(Command cmd) {
        final Instant now = Instant.now(clock);

        // 0) validate input (return outcome, don’t throw)
        if (cmd == null || cmd.name() == null || cmd.name().isBlank()
                || cmd.gender() == null || cmd.gender().isBlank()
                || cmd.birthDate() == null) {
            return new Result(Outcome.INVALID_INPUT, false, cmd == null ? null : cmd.sessionId(), SignupSessionState.DRAFT);
        }

        final UUID sessionId = cmd.sessionId();
        if (sessionId == null) {
            return new Result(Outcome.NO_SESSION, false, null, SignupSessionState.EXPIRED);
        }

        final SignupSession session = signupSessionStorePort.findActiveById(sessionId, now).orElse(null);
        if (session == null) {
            return new Result(Outcome.NO_SESSION, false, sessionId, SignupSessionState.EXPIRED);
        }

        // If your store already filters expired, this is optional:
        if (session.expireIfNeeded(now)) {
            signupSessionStorePort.save(session);
            return new Result(Outcome.NO_SESSION, false, sessionId, SignupSessionState.EXPIRED);
        }

        // 1) gate by derived state (or let domain throw and map)
        if (session.getState() != SignupSessionState.PROFILE_PENDING
                && session.getState() != SignupSessionState.PROFILE_READY) {
            return new Result(Outcome.INVALID_STATE, false, sessionId, session.getState());
        }

        // 2) domain command + persist
        session.submitProfile(cmd.name(), cmd.gender(), cmd.birthDate(), now);
        signupSessionStorePort.save(session);

        return new Result(Outcome.UPDATED, true, sessionId, session.getState());
    }
}