package com.timeeconomy.auth_service.domain.port.out;

public interface UserProfileSyncPort {

    void createUserProfile(Long userId, String email);
}