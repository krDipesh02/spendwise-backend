package com.spendwise.service;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.service.UserProfileService;
import com.spendwise.utils.AuthenticatedUser;
import com.spendwise.utils.AuthenticationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CurrentUserService {

    private final UserProfileService userProfileService;

    public CurrentUserService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    public UserProfile getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new EntityNotFoundException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            log.debug("Resolving current user from AuthenticatedUser principal userId={}", authenticatedUser.getUserId());
            return userProfileService.getById(authenticatedUser.getUserId());
        }
        if (principal instanceof OAuth2User oauth2User) {
            String googleSubject = oauth2User.getAttribute("sub");
            log.debug("Resolving current user from OAuth principal subject={}", googleSubject);
            return userProfileService.getByGoogleSubject(googleSubject);
        }
        log.error("Unsupported authentication principal type={}", principal.getClass().getName());
        throw new EntityNotFoundException("Unsupported authentication principal");
    }

    public AuthenticationType getAuthenticationType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found while resolving authentication type");
            throw new EntityNotFoundException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            log.debug("Resolved authentication type={} for userId={}",
                    authenticatedUser.getAuthenticationType(), authenticatedUser.getUserId());
            return authenticatedUser.getAuthenticationType();
        }
        if (principal instanceof OAuth2User) {
            log.debug("Resolved authentication type=GOOGLE");
            return AuthenticationType.GOOGLE;
        }
        log.error("Unsupported authentication principal type={} while resolving authentication type", principal.getClass().getName());
        throw new EntityNotFoundException("Unsupported authentication principal");
    }
}
