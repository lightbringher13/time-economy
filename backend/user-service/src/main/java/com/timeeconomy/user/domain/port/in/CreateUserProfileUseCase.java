package com.timeeconomy.user.domain.port.in;

import java.time.LocalDate;

public interface CreateUserProfileUseCase {

    record Command(
            Long userId,
            String email,
            String name,
            String gender,
            LocalDate birthDate,
            String phoneNumber
    ) {}

    void createProfile(Command command);
}