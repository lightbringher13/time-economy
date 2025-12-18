package com.timeeconomy.auth_service.domain.common.security.port;

public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}