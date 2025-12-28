package com.timeeconomy.user.application.userprofile.service;

import com.timeeconomy.contracts.auth.v1.AuthUserRegisteredV1;
import com.timeeconomy.user.application.userprofile.port.in.HandleAuthUserRegisteredUseCase;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HandleAuthUserRegisteredService implements HandleAuthUserRegisteredUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;

    @Override
    @Transactional
    public void handle(AuthUserRegisteredV1 event) {

        Long userId = event.getUserId();

        String email = toStr(event.getEmail());
        String name = toStr(event.getName());
        String phoneNumber = toStr(event.getPhoneNumber());
        String gender = toStr(event.getGender());

        // generated getter returns LocalDate (nullable)
        LocalDate birthDate = event.getBirthDateEpochDays();

        // generated getter returns Instant
        Instant occurredAt = event.getOccurredAtEpochMillis();

        UserProfile profile = userProfileRepositoryPort.findById(userId)
                .orElseGet(() -> UserProfile.createFromAuthUserRegistered(
                        userId,
                        email,
                        name,
                        phoneNumber,
                        birthDate,
                        gender,
                        occurredAt
                ));

        profile.applyAuthUserRegistered(
                email,
                name,
                phoneNumber,
                birthDate,
                gender,
                occurredAt
        );

        userProfileRepositoryPort.save(profile);
    }

    private static String toStr(Object v) {
        return v == null ? null : v.toString();
    }
}