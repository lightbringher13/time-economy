package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.domain.port.out.EmailVerificationMailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailVerificationMailAdapter implements EmailVerificationMailPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@timeeconomy.local}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String email, String code) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setFrom(fromEmail);
            msg.setSubject("[TimeEconomy] 이메일 인증 코드");
            msg.setText("""
                    안녕하세요, TimeEconomy 입니다.

                    이메일 인증 코드를 입력해 주세요.

                        %s

                    코드는 10분 동안 유효합니다.
                    본인이 요청한 인증이 아니라면 이 메일을 무시하세요.
                    """.formatted(code));

            mailSender.send(msg);
            log.info("[EmailVerification] Email sent to={} via Brevo SMTP", email);

        } catch (Exception ex) {
            log.error("[EmailVerification] Failed to send email to {}", email, ex);
        }
    }
}