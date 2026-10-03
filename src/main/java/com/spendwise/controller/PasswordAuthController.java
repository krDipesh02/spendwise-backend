package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.PasswordLoginRequest;
import com.spendwise.dto.request.PasswordRegisterRequest;
import com.spendwise.dto.service.UserProfileService;
import com.spendwise.service.JwtAuthService;
import com.spendwise.service.TelegramCredentialSetupService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/password")
@Slf4j
public class PasswordAuthController {

    private final UserProfileService userProfileService;
    private final JwtAuthService jwtAuthService;
    private final TelegramCredentialSetupService credentialSetupService;

    public PasswordAuthController(UserProfileService userProfileService, JwtAuthService jwtAuthService,
                                  TelegramCredentialSetupService credentialSetupService) {
        this.userProfileService = userProfileService;
        this.jwtAuthService = jwtAuthService;
        this.credentialSetupService = credentialSetupService;
    }

    @PostMapping("/telegram-setup")
    public Map<String, String> setupTelegramCredentials(@Valid @RequestBody TelegramSetupRequest request) {
        credentialSetupService.configureCredentials(request.token(), request.username(), request.password());
        return Map.of("status", "credentials_configured");
    }

    public record TelegramSetupRequest(String token, String username, String password) { }

    /**
     * Registers a new password-based account and authenticates the resulting session.
     *
     * @param request contains the username, password, and display name for the new account
     * @param httpRequest the incoming HTTP request used to persist the security context
     * @param httpResponse the outgoing HTTP response used to persist the security context
     * @return the created user profile
     */
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody PasswordRegisterRequest request,
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
        var tokens = jwtAuthService.createLogin(user);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, jwtAuthService.cookie(tokens.refreshToken(), jwtAuthService.getRefreshTokenTtlSeconds()).toString());
        log.info("Registered password user userId={}", user.getId());
        return response(tokens);
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
    public Map<String, Object> login(@Valid @RequestBody PasswordLoginRequest request,
                                HttpServletResponse httpResponse) {
        log.info("Authenticating password login for username={}", request.getUsername());
        UserProfile user = userProfileService.getByUsername(request.getUsername());
        if (!userProfileService.matchesPassword(user, request.getPassword())) {
            log.error("Password login failed for username={}", request.getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        var tokens = jwtAuthService.createLogin(user);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, jwtAuthService.cookie(tokens.refreshToken(), jwtAuthService.getRefreshTokenTtlSeconds()).toString());
        log.info("Password login succeeded for userId={}", user.getId());
        return response(tokens);
    }

    private Map<String, Object> response(JwtAuthService.TokenResult tokens) {
        return Map.of("accessToken", tokens.accessToken(), "tokenType", "Bearer", "expiresIn", tokens.expiresIn(),
                "user", Map.of("id", tokens.user().getId().toString(), "displayName", tokens.user().getDisplayName(),
                        "username", tokens.user().getUsername() == null ? "" : tokens.user().getUsername(),
                        "role", tokens.user().getApplicationRole().name(), "scopes", tokens.scopes()));
    }
}
