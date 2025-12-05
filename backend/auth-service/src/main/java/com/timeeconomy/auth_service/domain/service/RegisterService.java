package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.exception.EmailNotVerifiedException;
import com.timeeconomy.auth_service.domain.exception.PhoneNotVerifiedException;
import com.timeeconomy.auth_service.domain.model.AuthUser;
import com.timeeconomy.auth_service.domain.model.SignupSession;
import com.timeeconomy.auth_service.domain.port.in.RegisterUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.PasswordEncoderPort;
import com.timeeconomy.auth_service.domain.port.out.UserProfileSyncPort;
import com.timeeconomy.auth_service.domain.port.out.SignupSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final UserProfileSyncPort userProfileSyncPort;
    private final SignupSessionRepositoryPort signupSessionRepositoryPort;   // ⭐ NEW

    @Override
    public RegisterResult register(RegisterCommand command) {
        // 0) 세션 필수로 요구 (원하면 optional 로 바꿀 수 있음)
        if (command.signupSessionId() == null) {
            throw new SignupSessionNotFoundException("Missing signup session");
        }

        LocalDateTime now = LocalDateTime.now();

        SignupSession session = signupSessionRepositoryPort
                .findActiveById(command.signupSessionId(), now)
                .orElseThrow(() ->
                        new SignupSessionNotFoundException("Signup session not found or expired"));

        // 1) 이메일 normalize (소문자 + trim)
        String email = normalizeEmail(command.email());

        // 세션의 이메일과 요청 이메일이 다르면 문제
        if (session.getEmail() == null || !session.getEmail().equalsIgnoreCase(email)) {
            throw new SignupSessionNotFoundException("Email mismatch between signup session and request");
        }

        // 2) 이메일 인증 여부 체크
        if (!session.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email must be verified to register");
        }

        // 3) (선택) 전화번호 인증도 필수로 할지
        if (!session.isPhoneVerified()) {
            throw new PhoneNotVerifiedException("Phone number must be verified to register");
        }

        // 4) 중복 이메일 체크
        authUserRepositoryPort.findByEmail(email).ifPresent(existing -> {
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // 5) 비밀번호 해시
        String passwordHash = passwordEncoderPort.encode(command.password());

        // 6) 도메인 모델 생성
        AuthUser user = new AuthUser(
                email,
                passwordHash,
                command.phoneNumber()
        );

        // 세션 기준으로 인증 상태 반영
        user.setEmailVerified(true);                // 이미 인증된 상태에서 가입
        user.setPhoneVerified(session.isPhoneVerified());

        // 7) 저장
        AuthUser saved = authUserRepositoryPort.save(user);

        // 8) User-service 쪽 profile 생성 요청
        userProfileSyncPort.createUserProfile(
                new UserProfileSyncPort.CreateUserProfileCommand(
                        saved.getId(),
                        saved.getEmail(),
                        command.name(),
                        command.gender(),
                        command.birthDate(),
                        saved.getPhoneNumber()
                )
        );

        // 9) 세션 완료 처리
        session.markCompleted(now);
        signupSessionRepositoryPort.save(session);

        // 10) 결과 리턴
        return new RegisterResult(saved.getId(), saved.getEmail());
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }
}