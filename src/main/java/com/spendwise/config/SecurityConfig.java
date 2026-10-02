package com.spendwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                            TelegramServiceAuthenticationFilter telegramServiceAuthenticationFilter,
                                            GoogleOAuthSuccessHandler googleOAuthSuccessHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .addFilterBefore(telegramServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/auth/google/**", "/auth/password/**", "/auth/session", "/auth/logout").permitAll()
                        // Internal/admin endpoints validate dedicated credentials in their handlers.
                        .requestMatchers("/internal/telegram/**", "/admin/telegram/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth.successHandler(googleOAuthSuccessHandler))
                .build();
    }

    @Bean
    FilterRegistrationBean<TelegramServiceAuthenticationFilter> telegramServiceFilterRegistration(
            TelegramServiceAuthenticationFilter filter) {
        FilterRegistrationBean<TelegramServiceAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Run only inside the Spring Security chain.
        return registration;
    }

    @Bean
    FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyFilterRegistration(ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Run only inside the Spring Security chain.
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
