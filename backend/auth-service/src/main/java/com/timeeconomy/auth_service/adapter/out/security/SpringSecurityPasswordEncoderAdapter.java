package com.timeeconomy.auth_service.adapter.out.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth_service.domain.common.security.port.PasswordEncoderPort;

@Component
@RequiredArgsConstructor
public class SpringSecurityPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder delegate;

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}