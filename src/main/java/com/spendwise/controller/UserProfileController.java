package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.UpdateProfileRequest;
import com.spendwise.dto.response.UserProfileDto;
import com.spendwise.service.CurrentUserService;
import com.spendwise.dto.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@Slf4j
public class UserProfileController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    public UserProfileController(CurrentUserService currentUserService, UserProfileService userProfileService) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    /**
     * Returns the authenticated user's profile.
     *
     * @return the current user's profile details
     */
    @GetMapping
    public UserProfileDto getProfile() {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Fetching profile for userId={}", user.getId());
        return UserProfileDto.from(user);
    }

    /**
     * Updates mutable fields on the authenticated user's profile.
     *
     * @param request contains the profile fields to persist
     * @return the updated user profile
     */
    @PutMapping
    public UserProfileDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Updating profile for userId={}", user.getId());
        return UserProfileDto.from(userProfileService.updateProfile(
                user.getId(),
                request.getDisplayName(),
                request.getBaseCurrency(),
                request.getTimezone(),
                request.getMonthlyLimit()
        ));
    }
}
