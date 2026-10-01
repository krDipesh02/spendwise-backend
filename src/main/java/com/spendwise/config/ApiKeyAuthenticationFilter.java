package com.spendwise.config;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.service.UserApiKeyService;
import com.spendwise.utils.AuthenticatedUser;
import com.spendwise.utils.AuthenticationType;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final UserApiKeyService userApiKeyService;

    public ApiKeyAuthenticationFilter(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Skipping API key authentication for path={} because header is absent", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserProfile user = userApiKeyService.authenticate(apiKey.trim());
            log.info("Authenticated request via API key for userId={} path={}", user.getId(), request.getRequestURI());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(user.getId(), AuthenticationType.API_KEY),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_API_KEY_USER")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (EntityNotFoundException ex) {
            SecurityContextHolder.clearContext();
            log.error("API key authentication failed for path={}", request.getRequestURI(), ex);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
        }
    }
}
