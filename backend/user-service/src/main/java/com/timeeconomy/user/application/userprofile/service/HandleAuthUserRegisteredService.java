package com.timeeconomy.user.application.userprofile.service;

import com.timeeconomy.user.adapter.in.kafka.event.AuthUserRegisteredV1;
import com.timeeconomy.user.application.userprofile.port.in.HandleAuthUserRegisteredUseCase;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandleAuthUserRegisteredService implements HandleAuthUserRegisteredUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;

    @Override
    @Transactional
    public void handle(AuthUserRegisteredV1 event) {
        Long userId = event.userId();

        // 1) Load existing profile by userId (your domain uses id == auth userId)
        UserProfile profile = userProfileRepositoryPort.findById(userId)
                .orElseGet(() -> UserProfile.createFromAuthUserRegistered(
                        userId,
                        event.email(),
                        event.name(),
                        event.phoneNumber(),
                        event.birthDate(),
                        event.gender(),
                        event.occurredAt()
                ));

        // 2) Apply event to existing profile too (idempotent + keeps it synced)
        profile.applyAuthUserRegistered(
                event.email(),
                event.name(),
                event.phoneNumber(),
                event.birthDate(),
                event.gender(),
                event.occurredAt()
        );

        // 3) Persist
        userProfileRepositoryPort.save(profile);
    }
}