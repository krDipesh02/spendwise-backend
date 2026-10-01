package com.spendwise.controller;

import com.spendwise.dto.entity.PasswordResetToken;
import com.spendwise.dto.request.PasswordResetConfirmRequest;
import com.spendwise.dto.request.PasswordResetRequest;
import com.spendwise.dto.response.PasswordResetTokenResponse;
import com.spendwise.dto.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth/password/reset")
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Starts a password reset flow for a password-authenticated account.
     *
     * @param request contains the username requesting a reset token
     * @return the issued reset token and its expiration timestamp
     */
    @PostMapping("/request")
    public PasswordResetTokenResponse request(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Requesting password reset for username={}", request.getUsername());
        PasswordResetToken token = passwordResetService.requestReset(request.getUsername());
        return new PasswordResetTokenResponse(token.getToken(), token.getExpiresAt());
    }

    /**
     * Completes a password reset using a previously issued reset token.
     *
     * @param request contains the reset token and replacement password
     * @return a status payload confirming the password was updated
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        log.info("Confirming password reset");
        try {
            passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
        } catch (IllegalArgumentException ex) {
            log.error("Password reset confirmation failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return Map.of("status", "password_reset");
    }
}
