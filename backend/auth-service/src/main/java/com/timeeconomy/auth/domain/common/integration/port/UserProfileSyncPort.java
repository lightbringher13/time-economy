package com.timeeconomy.auth.domain.common.integration.port;

import java.time.LocalDate;

public interface UserProfileSyncPort {

    void createUserProfile(CreateUserProfileCommand command);

    record CreateUserProfileCommand(
            Long authUserId,    // user_profile.id (PK)
            String email,
            String name,
            String gender,
            LocalDate birthDate,
            String phoneNumber
    ) {}
}