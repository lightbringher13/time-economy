package com.timeeconomy.user.application.userprofile.service;

import com.timeeconomy.contracts.auth.v2.AuthUserRegisteredV2;
import com.timeeconomy.user.application.userprofile.port.in.HandleAuthUserRegisteredUseCase;
import com.timeeconomy.user.application.userprofile.port.out.SignupSessionInternalClientPort;
import com.timeeconomy.user.adapter.out.authclient.dto.CompletedSignupSessionResponse;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HandleAuthUserRegisteredService implements HandleAuthUserRegisteredUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final SignupSessionInternalClientPort signupSessionClient;

    @Override
    @Transactional
    public void handle(AuthUserRegisteredV2 event) {

        Long userId = event.getUserId();

        UUID signupSessionId = event.getSignupSessionId(); // nullable (union)

        CompletedSignupSessionResponse s = signupSessionClient.getCompletedSession(signupSessionId);

        if (!"COMPLETED".equals(s.state())) {
            // defensive; ideally auth-service endpoint guarantees COMPLETED only
            throw new IllegalStateException("Signup session is not COMPLETED. sessionId=" + signupSessionId + " state=" + s.state());
        }

        Instant occurredAt = event.getOccurredAtEpochMillis(); // Avro logical type -> Instant in generated code

        UserProfile profile = userProfileRepositoryPort.findById(userId)
                .orElseGet(() -> UserProfile.createFromAuthUserRegistered(
                        userId,
                        s.email(),
                        s.name(),
                        s.phoneNumber(),
                        s.birthDate(),
                        s.gender(),
                        occurredAt
                ));

        profile.applyAuthUserRegistered(
                s.email(),
                s.name(),
                s.phoneNumber(),
                s.birthDate(),
                s.gender(),
                occurredAt
        );

        userProfileRepositoryPort.save(profile);
    }
}