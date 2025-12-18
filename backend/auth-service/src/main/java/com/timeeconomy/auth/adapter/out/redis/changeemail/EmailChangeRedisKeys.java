package com.timeeconomy.auth.adapter.out.redis.changeemail;

public final class EmailChangeRedisKeys {
    private EmailChangeRedisKeys() {}

    public static String requestKey(Long requestId) {
        return "emailchg:req:" + requestId;
    }

    public static String activeByUserKey(Long userId) {
        return "emailchg:active:user:" + userId;
    }
}