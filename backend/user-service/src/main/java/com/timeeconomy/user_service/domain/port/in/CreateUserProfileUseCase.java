package com.timeeconomy.user_service.domain.port.in;

public interface CreateUserProfileUseCase {

    record Command(
            Long userId,
            String email
    ) {}

    void createProfile(Command command);
}