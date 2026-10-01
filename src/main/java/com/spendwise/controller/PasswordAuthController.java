package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.PasswordLoginRequest;
import com.spendwise.dto.request.PasswordRegisterRequest;
import com.spendwise.dto.response.UserProfileDto;
import com.spendwise.dto.service.UserProfileService;
import com.spendwise.utils.AuthenticatedUser;
import com.spendwise.utils.AuthenticationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/password")
@Slf4j
public class PasswordAuthController {

    private final UserProfileService userProfileService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public PasswordAuthController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Registers a new password-based account and authenticates the resulting session.
     *
     * @param request contains the username, password, and display name for the new account
     * @param httpRequest the incoming HTTP request used to persist the security context
     * @param httpResponse the outgoing HTTP response used to persist the security context
     * @return the created user profile
     */
    @PostMapping("/register")
    public UserProfileDto register(@Valid @RequestBody PasswordRegisterRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        log.info("Registering password user username={}", request.getUsername());
        UserProfile user;
        try {
            user = userProfileService.registerPasswordUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getDisplayName()
            );
        } catch (IllegalArgumentException ex) {
            log.error("Password registration failed for username={}", request.getUsername(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        authenticateSession(user, httpRequest, httpResponse);
        log.info("Registered password user userId={}", user.getId());
        return UserProfileDto.from(user);
    }

    /**
     * Authenticates a password-based account and stores the login in the current HTTP session.
     *
     * @param request contains the username and password credentials to validate
     * @param httpRequest the incoming HTTP request used to persist the security context
     * @param httpResponse the outgoing HTTP response used to persist the security context
     * @return the authenticated user profile
     */
    @PostMapping("/login")
    public UserProfileDto login(@Valid @RequestBody PasswordLoginRequest request,
                                HttpServletRequest httpRequest,
                                HttpServletResponse httpResponse) {
        log.info("Authenticating password login for username={}", request.getUsername());
        UserProfile user = userProfileService.getByUsername(request.getUsername());
        if (!userProfileService.matchesPassword(user, request.getPassword())) {
            log.error("Password login failed for username={}", request.getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        authenticateSession(user, httpRequest, httpResponse);
        log.info("Password login succeeded for userId={}", user.getId());
        return UserProfileDto.from(user);
    }

    private void authenticateSession(UserProfile user,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        log.debug("Persisting password-authenticated session for userId={}", user.getId());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(user.getId(), AuthenticationType.PASSWORD),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PASSWORD_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);
    }
}
