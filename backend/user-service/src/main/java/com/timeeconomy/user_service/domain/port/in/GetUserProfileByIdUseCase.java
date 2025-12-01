package com.timeeconomy.user_service.domain.port.in;

import com.timeeconomy.user_service.domain.model.UserProfile;

public interface GetUserProfileByIdUseCase {

    UserProfile getById(Long userId);
}