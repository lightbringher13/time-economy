package com.timeeconomy.auth_service.domain.signupsession.service;

import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth_service.domain.signupsession.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth_service.domain.signupsession.port.out.SignupSessionStorePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSignupProfileService implements UpdateSignupProfileUseCase {

    private final SignupSessionStorePort signupSessionRepositoryPort;

    @Override
    @Transactional
    public void updateProfile(Command cmd) {
        UUID sessionId = cmd.sessionId();
        LocalDateTime now = LocalDateTime.now();

        SignupSession session = signupSessionRepositoryPort
                .findActiveById(sessionId, now)
                .orElseThrow(() -> new SignupSessionNotFoundException(sessionId));

        // 1) 이메일: 아직 검증 안 된 경우에만 변경 허용
        if (cmd.email() != null && !cmd.email().isBlank() && !session.isEmailVerified()) {
            session.updateEmail(cmd.email(), now);   // 이미 있는 도메인 메서드 재사용
        }

        // phone update (only allowed before verification)
        if (cmd.phoneNumber() != null 
                && !cmd.phoneNumber().isBlank() 
                && !session.isPhoneVerified()) {

            session.setPhoneNumber(cmd.phoneNumber());
        }

        

        // 2) 프로필 필드 업데이트 (지금 네 도메인 메서드 그대로 활용)
        session.updateProfile(
                cmd.name(),
                cmd.gender(),
                cmd.birthDate(),
                now
        );

        signupSessionRepositoryPort.save(session);
    }
}