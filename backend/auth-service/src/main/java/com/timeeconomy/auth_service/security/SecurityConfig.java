package com.timeeconomy.auth_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // For APIs, usually disable CSRF (we’ll revisit later if needed)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll() // ✅ public
                .anyRequest().authenticated()               // everything else protected
            );

        // Also disable default login page, form login, etc. (optional)
        // .httpBasic(Customizer.withDefaults()); // if you want basic auth

        return http.build();
    }
}