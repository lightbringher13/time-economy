package com.timeeconomy.auth_service.adapter.out.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth_service.domain.passwordreset.port.out.PasswordResetMailPort;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class BrevoPasswordResetMailAdapter implements PasswordResetMailPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@timeeconomy.local}")
    private String fromEmail;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void sendPasswordResetLink(String email, String rawToken) {
        try {
            // 1) reset 링크 만들기
            String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + encodedToken;

            // 2) 메일 내용 구성 (이메일 인증과 같은 스타일, 한국어)
            String subject = "[TimeEconomy] 비밀번호 재설정 안내";
            String text = """
                    안녕하세요, TimeEconomy 입니다.

                    아래 링크를 클릭하여 비밀번호를 재설정해 주세요.

                        %s

                    이 링크는 일정 시간(예: 30분) 동안만 유효합니다.
                    본인이 요청한 비밀번호 재설정이 아니라면 이 메일을 무시하셔도 됩니다.
                    """.formatted(resetUrl);

            // 3) 메일 전송
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setFrom(fromEmail);
            msg.setSubject(subject);
            msg.setText(text);

            mailSender.send(msg);
            log.info("[PasswordReset] Email sent to={} via Brevo SMTP, url={}", email, resetUrl);

        } catch (Exception ex) {
            log.error("[PasswordReset] Failed to send reset email to {}", email, ex);
            // 필요하면 도메인에 알려줄 custom exception 던져도 됨
        }
    }
}