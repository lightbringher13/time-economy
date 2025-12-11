package com.timeeconomy.auth_service.adapter.out.mail;

import com.timeeconomy.auth_service.domain.port.out.EmailNotificationPort;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;


@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements EmailNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@timeeconomy.local}")
    private String fromEmail;

    @Override
    public void sendSecurityAlert(Long userId, String subject, String message) {
        // TODO: 나중에 실제 이메일 서비스로 교체
        log.warn("SECURITY EMAIL to userId={} | subject={} | message={}", userId, subject, message);
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