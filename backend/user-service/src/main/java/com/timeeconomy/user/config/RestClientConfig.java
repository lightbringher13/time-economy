package com.timeeconomy.user.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Qualifier("authInternalRestClient")
    public RestClient authInternalRestClient(
            @Value("${auth.internal.base-url}") String baseUrl,
            @Value("${auth.internal.token}") String internalToken
    ) {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(2_000);
        rf.setReadTimeout(5_000);

        return RestClient.builder()
                .baseUrl(baseUrl) // MUST be like http://auth-service:8080
                .defaultHeader("X-Internal-Token", internalToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestFactory(rf)
                .build();
    }
}