package com.timeeconomy.user_service.adapter.out.jpa.mapper;

import com.timeeconomy.user_service.adapter.out.jpa.entity.UserProfileEntity;
import com.timeeconomy.user_service.domain.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileEntity toEntity(UserProfile d) {
        if (d == null) {
            return null;
        }

        return new UserProfileEntity(
                d.getId(),
                d.getEmail(),
                d.getName(),
                d.getPhoneNumber(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    public UserProfile toDomain(UserProfileEntity e) {
        if (e == null) {
            return null;
        }

        return new UserProfile(
                e.getId(),
                e.getEmail(),
                e.getName(),
                e.getPhoneNumber(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}