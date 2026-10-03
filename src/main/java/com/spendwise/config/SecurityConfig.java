package com.spendwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import jakarta.servlet.DispatcherType;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.spendwise.service.JwtAuthService;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                            TelegramServiceAuthenticationFilter telegramServiceAuthenticationFilter,
                                            JwtAuthService jwtAuthService,
                                            GoogleOAuthSuccessHandler googleOAuthSuccessHandler) throws Exception {
        return http
                // Browser APIs use bearer access tokens; the refresh cookie is HttpOnly + SameSite.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .addFilterBefore(telegramServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtAuthService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(registry -> registry
                        // Let Spring Boot render the original MVC error status/body instead of
                        // re-authenticating the container's internal ERROR dispatch.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(path("/actuator/health")).permitAll()
                        .requestMatchers(path("/oauth2/**", "/login/**", "/auth/google/**", "/auth/password/**", "/auth/refresh", "/auth/logout")).permitAll()
                        .requestMatchers(path("/internal/telegram/**")).permitAll()
                        .requestMatchers(path("/admin/telegram/**")).authenticated()
                        .requestMatchers(path("/auth/session", "/auth/me")).hasAuthority("ROLE_JWT_USER")
                        .requestMatchers(methodPath(HttpMethod.GET, "/profile")).hasAnyAuthority("SCOPE_profile:read", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.PUT, "/profile")).hasAnyAuthority("SCOPE_profile:write", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.GET, "/expenses/**", "/budgets/**", "/categories/**", "/analytics/**", "/recurring-expenses/**", "/reminders/**", "/receipts/**"))
                                .hasAnyAuthority("SCOPE_expenses:read", "SCOPE_budgets:read", "SCOPE_categories:read", "SCOPE_analytics:read", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.POST, "/expenses/**", "/recurring-expenses/**", "/receipts/**", "/reminders/**"))
                                .hasAnyAuthority("SCOPE_expenses:write", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.POST, "/budgets/**")).hasAnyAuthority("SCOPE_budgets:write", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.POST, "/categories/**")).hasAnyAuthority("SCOPE_categories:write", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.PUT, "/expenses/**", "/budgets/**", "/categories/**", "/recurring-expenses/**"))
                                .hasAnyAuthority("SCOPE_expenses:write", "SCOPE_budgets:write", "SCOPE_categories:write", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(methodPath(HttpMethod.DELETE, "/expenses/**", "/recurring-expenses/**"))
                                .hasAnyAuthority("SCOPE_expenses:write", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE")
                        .requestMatchers(path("/api-keys", "/api-keys/**")).hasAnyAuthority("SCOPE_api-keys:manage", "ROLE_API_KEY_USER")
                        // Reject session-only principals on business APIs; legacy API keys and trusted bot identities remain supported.
                        .anyRequest().hasAnyAuthority("ROLE_JWT_USER", "ROLE_API_KEY_USER", "ROLE_TELEGRAM_SERVICE"))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(oauth -> oauth.successHandler(googleOAuthSuccessHandler))
                .build();
    }

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static RequestMatcher path(String... patterns) {
        return request -> {
            String path = pathWithoutContext(request);
            return Arrays.stream(patterns).anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
        };
    }

    private static RequestMatcher methodPath(HttpMethod method, String... patterns) {
        return request -> method.matches(request.getMethod()) && path(patterns).matches(request);
    }

    private static String pathWithoutContext(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        return !contextPath.isEmpty() && path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
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
