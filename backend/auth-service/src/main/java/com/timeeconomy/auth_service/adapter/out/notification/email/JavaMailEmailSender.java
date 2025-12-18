package com.timeeconomy.auth_service.adapter.out.notification.email;

import com.timeeconomy.auth_service.domain.common.notification.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@timeeconomy.local}")
    private String from;

    @Override
    public void send(NotificationMessage message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(message.destination());
            mail.setFrom(from);
            mail.setSubject(resolveSubject(message));
            mail.setText(renderBody(message));

            mailSender.send(mail);

            log.info("[EmailSender] Sent email to={} template={}",
                    message.destination(), message.templateKey());

        } catch (Exception ex) {
            log.error("[EmailSender] Failed to send email to={}", message.destination(), ex);
        }
    }

    private String resolveSubject(NotificationMessage message) {
        return switch (message.templateKey()) {
            case "VERIFICATION_OTP" -> "[TimeEconomy] Verification code";
            case "VERIFICATION_LINK" -> "[TimeEconomy] Verification link";
            case "EMAIL_CHANGED_OLD" -> "[TimeEconomy] Your email was changed";
            case "EMAIL_CHANGED_NEW" -> "[TimeEconomy] Welcome (email verified)";
            case "SECURITY_REFRESH_TOKEN_REUSE" -> "[TimeEconomy] Security alert: suspicious activity";
            default -> "[TimeEconomy] Notification";
        };
    }

    private String renderBody(NotificationMessage msg) {
        return switch (msg.templateKey()) {
            case "SECURITY_REFRESH_TOKEN_REUSE" -> """
                    We detected suspicious refresh token reuse.

                    familyId: %s
                    ipAddress: %s
                    userAgent: %s
                    deviceInfo: %s

                    If this wasn't you, please sign in again and change your password.
                    """.formatted(
                    msg.vars().get("familyId"),
                    msg.vars().get("ipAddress"),
                    msg.vars().get("userAgent"),
                    msg.vars().get("deviceInfo")
            );
            default -> msg.vars().toString();
        };
    }
}