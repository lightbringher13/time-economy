package com.timeeconomy.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BrevoRestClientConfig {

    @Bean
    public RestClient brevoRestClient(
            @Value("${brevo.base-url}") String baseUrl
    ) {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(3_000);
        rf.setReadTimeout(10_000);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(rf)
                .build();
    }
}