package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateSignupProfileService implements UpdateSignupProfileUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final Clock clock;
    
    @Override
    @Transactional
    public void updateProfile(Command cmd) {
        Instant now = Instant.now(clock);

        SignupSession session = signupSessionStorePort
            .findActiveById(cmd.sessionId(), now)
            .orElseThrow(() -> new SignupSessionNotFoundException(cmd.sessionId()));

        // expire guard (optional but recommended)
        if (session.expireIfNeeded(now)) {
            signupSessionStorePort.save(session);
            throw new SignupSessionNotFoundException(cmd.sessionId()); // or InvalidStateException
        }

        // ✅ only allowed at PROFILE_PENDING
        session.submitProfile(cmd.name(), cmd.gender(), cmd.birthDate(), now);

        signupSessionStorePort.save(session);
    }
}