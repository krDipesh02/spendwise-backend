package com.spendwise.controller;

import com.spendwise.entity.UserProfile;
import com.spendwise.service.CurrentUserService;
import com.spendwise.security.principal.AuthenticatedUser;
import com.spendwise.security.principal.AuthenticationType;
import com.spendwise.service.JwtAuthService;
import com.spendwise.security.BrowserOriginVerifier;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthSessionController {

    private final CurrentUserService currentUserService;
    private final JwtAuthService jwtAuthService;
    private final BrowserOriginVerifier originVerifier;

    public AuthSessionController(CurrentUserService currentUserService, JwtAuthService jwtAuthService,
                                 BrowserOriginVerifier originVerifier) {
        this.currentUserService = currentUserService;
        this.jwtAuthService = jwtAuthService;
        this.originVerifier = originVerifier;
    }

    /**
     * Returns the current authenticated session details for either Google OAuth or password login.
     *
     * @return a map describing whether the request is authenticated and the current user's session metadata
     */
    @GetMapping({"/session", "/me"})
    public Map<String, Object> session() {
        log.info("Fetching auth session details");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated session found");
            return Map.of("authenticated", false);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser || principal instanceof OAuth2User) {
            UserProfile user = currentUserService.getCurrentUser();
            log.debug("Resolved authenticated user userId={} authType={}", user.getId(), currentUserService.getAuthenticationType());
            return Map.of(
                    "authenticated", true,
                    "authType", currentUserService.getAuthenticationType().name(),
                    "userId", user.getId().toString(),
                    "displayName", user.getDisplayName(),
                    "username", user.getUsername() == null ? "" : user.getUsername(),
                    "email", user.getEmail() == null ? "" : user.getEmail(),
                    "role", user.getApplicationRole().name(),
                    "scopes", jwtAuthService.scopes(user.getApplicationRole())
            );
        }

        log.error("Encountered unsupported authentication principal type={}", principal.getClass().getName());
        return Map.of("authenticated", false);
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@CookieValue(name = JwtAuthService.REFRESH_COOKIE, required = false) String rawToken,
                                       HttpServletResponse response, HttpServletRequest request) {
        originVerifier.requireSameOrigin(request);
        var tokens = jwtAuthService.rotate(rawToken);
        response.addHeader(HttpHeaders.SET_COOKIE, jwtAuthService.cookie(tokens.refreshToken(), jwtAuthService.getRefreshTokenTtlSeconds()).toString());
        return Map.of("accessToken", tokens.accessToken(), "tokenType", "Bearer", "expiresIn", tokens.expiresIn());
    }

    /**
     * Invalidates the active HTTP session and clears the Spring Security context.
     *
     * @param request the current HTTP request used to locate the existing session
     * @return a status payload confirming logout
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        originVerifier.requireSameOrigin(request);
        log.info("Logging out current session");
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) if (JwtAuthService.REFRESH_COOKIE.equals(cookie.getName())) refreshToken = cookie.getValue();
        jwtAuthService.revoke(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, jwtAuthService.cookie("", 0).toString());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Map.of("status", "logged_out");
    }
}
