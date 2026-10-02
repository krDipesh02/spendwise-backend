package com.spendwise.controller;

import com.spendwise.config.ServiceTokenVerifier;
import com.spendwise.model.TelegramAccountStatus;
import com.spendwise.service.TelegramAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/admin/telegram")
@Slf4j
public class TelegramAdminController {
    private final TelegramAuthorizationService service;
    private final ServiceTokenVerifier tokenVerifier;

    public TelegramAdminController(TelegramAuthorizationService service, ServiceTokenVerifier tokenVerifier) {
        this.service = service;
        this.tokenVerifier = tokenVerifier;
    }

    @PostMapping("/invites")
    public InviteResponse createInvite(@RequestHeader(value = "Authorization", required = false) String authorization) {
        tokenVerifier.requireAdminToken(authorization);
        var created = service.createInvite();
        return new InviteResponse(created.inviteId(), created.inviteUrl(), created.expiresAt(), created.expiresInMinutes());
    }

    @GetMapping("/claims")
    public java.util.List<TelegramAuthorizationService.PendingClaim> pendingClaims(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        tokenVerifier.requireAdminToken(authorization);
        return service.listPendingClaims();
    }

    @PostMapping("/claims/{inviteId}/approve")
    public TelegramAuthorizationService.ApprovalResult approveClaim(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID inviteId) {
        tokenVerifier.requireAdminToken(authorization);
        return service.approveClaim(inviteId);
    }

    @PostMapping("/claims/{inviteId}/reject")
    public void rejectClaim(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @PathVariable UUID inviteId) {
        tokenVerifier.requireAdminToken(authorization);
        service.rejectClaim(inviteId);
    }

    @PostMapping("/invites/{inviteId}/revoke")
    public void revokeInvite(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @PathVariable UUID inviteId) {
        tokenVerifier.requireAdminToken(authorization);
        service.revokeInvite(inviteId);
    }

    @PatchMapping("/users/{telegramUserId}/status")
    public void setStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @PathVariable String telegramUserId, @Valid @RequestBody SetStatusRequest request) {
        tokenVerifier.requireAdminToken(authorization);
        service.setAccountStatus(telegramUserId, request.status());
    }

    public record SetStatusRequest(@NotNull TelegramAccountStatus status) {}
    public record InviteResponse(UUID inviteId, String inviteUrl, Instant expiresAt, int expiresInMinutes) {}
}
