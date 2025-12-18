package com.timeeconomy.auth_service.adapter.out.redis.signupsession;

import java.util.UUID;

public final class SignupSessionRedisKeys {
    private SignupSessionRedisKeys() {}

    public static String sessionKey(UUID id) {
        return "signup:sess:" + id;
    }

    public static String emailIndexKey(String emailNorm) {
        return "signup:idx:email:" + emailNorm;
    }
}