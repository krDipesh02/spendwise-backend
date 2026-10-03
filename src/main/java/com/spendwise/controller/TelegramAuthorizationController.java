package com.spendwise.controller;

import com.spendwise.config.ServiceTokenVerifier;
import com.spendwise.model.TelegramAccountStatus;
import com.spendwise.service.TelegramAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.spendwise.service.TelegramCredentialSetupService;

import java.util.UUID;

@RestController
@RequestMapping("/internal/telegram")
@Slf4j
public class TelegramAuthorizationController {
    private final TelegramAuthorizationService service;
    private final ServiceTokenVerifier tokenVerifier;
    private final TelegramCredentialSetupService credentialSetupService;

    public TelegramAuthorizationController(TelegramAuthorizationService service, ServiceTokenVerifier tokenVerifier,
                                           TelegramCredentialSetupService credentialSetupService) {
        this.service = service;
        this.tokenVerifier = tokenVerifier;
        this.credentialSetupService = credentialSetupService;
    }

    @PostMapping("/users/{telegramUserId}/credential-setup")
    public CredentialSetupResponse credentialSetup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable String telegramUserId) {
        tokenVerifier.requireTelegramServiceToken(authorization);
        return new CredentialSetupResponse(credentialSetupService.issueSetupUrl(telegramUserId));
    }

    @GetMapping("/users/{telegramUserId}")
    public AuthorizationResponse lookup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String telegramUserId) {
        tokenVerifier.requireTelegramServiceToken(authorization);
        var result = service.getAuthorization(telegramUserId);
        return new AuthorizationResponse(result.status(), result.userId() == null ? null : result.userId().toString());
    }

    @PostMapping("/claim")
    public ClaimResponse claim(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @Valid @RequestBody ClaimRequest request) {
        tokenVerifier.requireTelegramServiceToken(authorization);
        var result = service.claim(request.telegramUserId(), request.inviteToken(), request.username(), request.firstName(), request.lastName());
        HttpStatus status = switch (result.status()) {
            case PENDING_APPROVAL, ALREADY_ACTIVE -> HttpStatus.OK;
            case INVALID_INVITE -> HttpStatus.NOT_FOUND;
            case INVITE_EXPIRED, INVITE_REVOKED -> HttpStatus.GONE;
            case INVITE_ALREADY_USED, TELEGRAM_ALREADY_LINKED -> HttpStatus.CONFLICT;
            case ACCOUNT_BLOCKED -> HttpStatus.FORBIDDEN;
        };
        if (!status.is2xxSuccessful()) throw new ResponseStatusException(status, result.status().name());
        return new ClaimResponse(result.status().name(), result.userId() == null ? null : result.userId().toString());
    }

    public record ClaimRequest(@NotBlank String telegramUserId, @NotBlank String inviteToken,
                               String username, String firstName, String lastName) {}
    public record AuthorizationResponse(String status, String userId) {}
    public record ClaimResponse(String status, String userId) {}
    public record CredentialSetupResponse(String setupUrl) {}
}
