package com.spendwise.security.filter;

import com.spendwise.service.JwtAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthService jwtAuthService;
    public JwtAuthenticationFilter(JwtAuthService jwtAuthService) { this.jwtAuthService = jwtAuthService; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return request.getHeader("X-Telegram-User-Id") != null || request.getHeader("X-API-Key") != null
                || path.contains("/internal/telegram/") || path.contains("/auth/password/")
                || path.endsWith("/auth/refresh") || path.endsWith("/auth/logout")
                || path.contains("/oauth2/") || path.contains("/login/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var authentication = jwtAuthService.authenticate(header.substring(7));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired access token");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
