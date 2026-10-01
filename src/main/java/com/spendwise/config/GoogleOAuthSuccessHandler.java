package com.spendwise.config;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserProfileService userProfileService;
    private final AuthProperties authProperties;

    public GoogleOAuthSuccessHandler(UserProfileService userProfileService,
                                     AuthProperties authProperties) {
        this.userProfileService = userProfileService;
        this.authProperties = authProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String googleSubject = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");
        String pictureUrl = oauthUser.getAttribute("picture");
        String displayName = oauthUser.getAttribute("name");

        log.info("Handling Google OAuth success for subject={} path={}", googleSubject, request.getRequestURI());
        UserProfile user = userProfileService.getOrCreateFromGoogle(
                googleSubject,
                email,
                Boolean.TRUE.equals(emailVerified),
                pictureUrl,
                displayName
        );

        log.info("Completed Google OAuth success for userId={}", user.getId());
        response.sendRedirect(authProperties.getGoogleSuccessRedirectUrl());
    }

}
