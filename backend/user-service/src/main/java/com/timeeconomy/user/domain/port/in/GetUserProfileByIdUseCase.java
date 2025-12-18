package com.timeeconomy.user.domain.port.in;

import com.timeeconomy.user.domain.model.UserProfile;

public interface GetUserProfileByIdUseCase {

    UserProfile getById(Long userId);
}