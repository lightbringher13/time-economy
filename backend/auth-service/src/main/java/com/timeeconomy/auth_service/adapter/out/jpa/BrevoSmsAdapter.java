package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.domain.port.out.PhoneVerificationSmsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")  
@RequiredArgsConstructor
@Slf4j
public class BrevoSmsAdapter implements PhoneVerificationSmsPort {

    private final RestClient.Builder restClientBuilder;

    @Value("${brevo.sms.api-key}")
    private String apiKey;

    @Value("${brevo.sms.sender-name}")
    private String senderName;

    @Override
    public void sendVerificationCode(String countryCode, String phoneNumber, String code) {

        var body = new SmsRequest(
                senderName,
                countryCode + phoneNumber,
                "[TimeEconomy] 인증코드: " + code
        );

        try {
            RestClient client = restClientBuilder.build();

            client.post()
                    .uri("https://api.brevo.com/v3/transactionalSMS/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SMS] Sent verification code to {}{}", countryCode, phoneNumber);

        } catch (Exception ex) {
            log.error("[SMS] Failed to send SMS to {}{}", countryCode, phoneNumber, ex);
            throw new IllegalStateException("Failed to send SMS", ex);
        }
    }

    private record SmsRequest(String sender, String recipient, String content) {}
}