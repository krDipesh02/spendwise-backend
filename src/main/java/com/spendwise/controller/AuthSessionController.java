package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.service.CurrentUserService;
import com.spendwise.utils.AuthenticatedUser;
import com.spendwise.utils.AuthenticationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthSessionController {

    private final CurrentUserService currentUserService;

    public AuthSessionController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /**
     * Returns the current authenticated session details for either Google OAuth or password login.
     *
     * @return a map describing whether the request is authenticated and the current user's session metadata
     */
    @GetMapping("/session")
    public Map<String, Object> session() {
        log.info("Fetching auth session details");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated session found");
            return Map.of("authenticated", false);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            UserProfile user = currentUserService.getCurrentUser();
            log.debug("Resolved OAuth session for userId={}", user.getId());
            return Map.of(
                    "authenticated", true,
                    "authType", AuthenticationType.GOOGLE.name(),
                    "userId", user.getId().toString(),
                    "name", Objects.requireNonNull(oauth2User.getAttribute("name")),
                    "email", Objects.requireNonNull(oauth2User.getAttribute("email")),
                    "sub", Objects.requireNonNull(oauth2User.getAttribute("sub"))
            );
        }

        if (principal instanceof AuthenticatedUser) {
            UserProfile user = currentUserService.getCurrentUser();
            log.debug("Resolved password/API-key session for userId={} authType={}", user.getId(), currentUserService.getAuthenticationType());
            return Map.of(
                    "authenticated", true,
                    "authType", currentUserService.getAuthenticationType().name(),
                    "userId", user.getId().toString(),
                    "displayName", user.getDisplayName()
            );
        }

        log.error("Encountered unsupported authentication principal type={}", principal.getClass().getName());
        return Map.of("authenticated", false);
    }

    /**
     * Invalidates the active HTTP session and clears the Spring Security context.
     *
     * @param request the current HTTP request used to locate the existing session
     * @return a status payload confirming logout
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        log.info("Logging out current session");
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Map.of("status", "logged_out");
    }
}
