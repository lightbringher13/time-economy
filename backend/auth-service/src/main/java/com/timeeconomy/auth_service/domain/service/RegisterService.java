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

        // (선택) 나중에 phoneNumber 중복 체크도 가능:
        // authUserRepositoryPort.findByPhoneNumber(...) 등

        // 3) 비밀번호 해시
        String passwordHash = passwordEncoderPort.encode(command.password());

        // 4) 도메인 모델 생성 (status = ACTIVE 기본, email/phoneVerified = false 기본)
        AuthUser user = new AuthUser(
                email,
                passwordHash,
                command.phoneNumber()   // ⭐️ NEW: phoneNumber 사용
        );

        // 5) 저장
        AuthUser saved = authUserRepositoryPort.save(user);

        // 6) User-service 쪽 profile 생성 요청
        userProfileSyncPort.createUserProfile(
                new UserProfileSyncPort.CreateUserProfileCommand(
                        saved.getId(),          // authUserId
                        saved.getEmail(),
                        command.name(),
                        command.gender(),
                        command.birthDate(),
                        saved.getPhoneNumber()
                )
        );

        // 7) 결과 리턴
        return new RegisterResult(saved.getId(), saved.getEmail());
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }
}