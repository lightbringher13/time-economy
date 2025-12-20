package com.timeeconomy.user.adapter.out.jpa.mapper;

import org.springframework.stereotype.Component;

import com.timeeconomy.user.adapter.out.jpa.entity.UserProfileEntity;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;

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
                d.getBirthDate(),   // ⭐ NEW
                d.getGender(),      // ⭐ NEW
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
                e.getBirthDate(),   // ⭐ NEW
                e.getGender(),      // ⭐ NEW
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}