package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.CreateApiKeyRequest;
import com.spendwise.dto.response.ApiKeyCreatedDto;
import com.spendwise.dto.response.ApiKeyDto;
import com.spendwise.service.CurrentUserService;
import com.spendwise.dto.service.UserApiKeyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api-keys")
@Slf4j
public class ApiKeyController {

    private final CurrentUserService currentUserService;
    private final UserApiKeyService userApiKeyService;

    public ApiKeyController(CurrentUserService currentUserService, UserApiKeyService userApiKeyService) {
        this.currentUserService = currentUserService;
        this.userApiKeyService = userApiKeyService;
    }

    /**
     * Creates a new API key for the authenticated user.
     *
     * @param request contains the display name to assign to the generated key
     * @return the generated API key payload, including the one-time secret value
     */
    @PostMapping
    public ApiKeyCreatedDto create(@Valid @RequestBody CreateApiKeyRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Creating API key for userId={} name={}", user.getId(), request.getName());
        return userApiKeyService.generate(user, request.getName());
    }

    /**
     * Lists active API keys belonging to the authenticated user.
     *
     * @return all non-revoked API keys for the current user
     */
    @GetMapping
    public List<ApiKeyDto> list() {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Listing API keys for userId={}", user.getId());
        return userApiKeyService.listActive(user).stream().map(ApiKeyDto::from).toList();
    }

    /**
     * Revokes an API key owned by the authenticated user.
     *
     * @param id the identifier of the API key to revoke
     */
    @DeleteMapping("/{id}")
    public void revoke(@PathVariable UUID id) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Revoking API key for userId={} apiKeyId={}", user.getId(), id);
        userApiKeyService.revoke(user, id);
    }
}
