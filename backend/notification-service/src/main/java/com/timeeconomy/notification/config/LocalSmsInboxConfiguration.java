package com.timeeconomy.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration(proxyBeanMethods = false)
@Profile("sms-inbox & sms-mock & (local | docker | test) & !prod")
@EnableConfigurationProperties(LocalSmsInboxProperties.class)
@RequiredArgsConstructor
public class LocalSmsInboxConfiguration implements WebMvcConfigurer {

    private final LocalSmsInboxTokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor);
    }
}
