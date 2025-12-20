package com.timeeconomy.user.domain.userprofile.port.in;

import com.timeeconomy.user.domain.userprofile.model.UserProfile;

public interface GetUserProfileByIdUseCase {

    UserProfile getById(Long userId);
}