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

    @Override
    public void notifyEmailChangedOldEmail(String oldEmail, String newEmail) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(oldEmail);
            msg.setFrom(fromEmail);
            msg.setSubject("[TimeEconomy] 계정 이메일 주소가 변경되었습니다");
            msg.setText("""
                    안녕하세요, TimeEconomy 입니다.

                    회원님의 계정 이메일 주소가 아래와 같이 변경되었습니다.

                        기존 이메일: %s
                        새 이메일:   %s

                    본인이 요청한 변경이 아니라면 즉시 고객센터로 문의해 주세요.
                    """.formatted(oldEmail, newEmail));

            mailSender.send(msg);
            log.info("[EmailChange] Old-email notification sent to={}", oldEmail);
        } catch (Exception ex) {
            log.error("[EmailChange] Failed to send old-email notification to {}", oldEmail, ex);
        }
    }

    @Override
    public void notifyEmailChangedNewEmail(String newEmail) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(newEmail);
            msg.setFrom(fromEmail);
            msg.setSubject("[TimeEconomy] 이메일 주소 변경이 완료되었습니다");
            msg.setText("""
                    안녕하세요, TimeEconomy 입니다.

                    회원님의 새 이메일 주소로 변경이 정상적으로 완료되었습니다.

                    이 메일을 요청한 적이 없다면 즉시 고객센터로 문의해 주세요.
                    """);

            mailSender.send(msg);
            log.info("[EmailChange] New-email notification sent to={}", newEmail);
        } catch (Exception ex) {
            log.error("[EmailChange] Failed to send new-email notification to {}", newEmail, ex);
        }
    }
}