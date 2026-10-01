package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.service.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/google")
@Slf4j
public class GoogleAuthController {

    private final CurrentUserService currentUserService;

    public GoogleAuthController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /**
     * Returns profile details for the currently authenticated Google OAuth user.
     *
     * @param oauth2User the authenticated OAuth principal resolved by Spring Security
     * @return a map describing authentication status and linked Google account metadata
     */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            log.debug("Google profile requested without authenticated OAuth user");
            return Map.of("authenticated", false);
        }
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Fetching Google profile for userId={}", user.getId());
        return Map.of(
                "authenticated", true,
                "userId", user.getId().toString(),
                "name", oauth2User.getAttribute("name"),
                "email", oauth2User.getAttribute("email"),
                "sub", oauth2User.getAttribute("sub"),
                "googleLinked", user.isGoogleLinked()
        );
    }
}
