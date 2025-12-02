package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth_service.domain.model.AuthUser;
import com.timeeconomy.auth_service.domain.port.in.RegisterUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.PasswordEncoderPort;
import com.timeeconomy.auth_service.domain.port.out.UserProfileSyncPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final UserProfileSyncPort userProfileSyncPort;

    @Override
    public RegisterResult register(RegisterCommand command) {
        // 1) 이메일 normalize (소문자 + trim)
        String email = normalizeEmail(command.email());

        // 2) 중복 이메일 체크
        authUserRepositoryPort.findByEmail(email).ifPresent(existing -> {
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // 3) 비밀번호 해시
        String passwordHash = passwordEncoderPort.encode(command.password());

        // 4) 도메인 모델 생성 (status = ACTIVE 기본)
        AuthUser user = new AuthUser(email, passwordHash);

        // 5) 저장
        AuthUser saved = authUserRepositoryPort.save(user);

        // 6) (나중에) 여기서 UserRegistered 이벤트 발행하면 됨
        userProfileSyncPort.createUserProfile(saved.getId(), saved.getEmail());

        return new RegisterResult(saved.getId(), saved.getEmail());
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }
}