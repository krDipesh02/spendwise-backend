package com.spendwise.controller;

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
import org.springframework.security.access.prepost.PreAuthorize;
import com.spendwise.service.CurrentUserService;
import com.spendwise.dto.service.AuditService;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/admin/telegram")
@Slf4j
public class TelegramAdminController {
    private final TelegramAuthorizationService service;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    public TelegramAdminController(TelegramAuthorizationService service, CurrentUserService currentUserService, AuditService auditService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @PostMapping("/invites")
    @PreAuthorize("hasAuthority('SCOPE_telegram:invites:manage')")
    public InviteResponse createInvite() {
        var created = service.createInvite();
        audit("TELEGRAM_INVITE_CREATE", created.inviteId().toString(), "expiresAt=" + created.expiresAt());
        return new InviteResponse(created.inviteId(), created.inviteUrl(), created.expiresAt(), created.expiresInMinutes());
    }

    @GetMapping("/claims")
    @PreAuthorize("hasAuthority('SCOPE_telegram:claims:read')")
    public java.util.List<TelegramAuthorizationService.PendingClaim> pendingClaims() {
        return service.listPendingClaims();
    }

    @PostMapping("/claims/{inviteId}/approve")
    @PreAuthorize("hasAuthority('SCOPE_telegram:claims:manage')")
    public TelegramAuthorizationService.ApprovalResult approveClaim(@PathVariable UUID inviteId) {
        var result = service.approveClaim(inviteId);
        audit("TELEGRAM_CLAIM_APPROVE", inviteId.toString(), "telegramUserId=" + result.telegramUserId());
        return result;
    }

    @PostMapping("/claims/{inviteId}/reject")
    @PreAuthorize("hasAuthority('SCOPE_telegram:claims:manage')")
    public void rejectClaim(@PathVariable UUID inviteId) {
        service.rejectClaim(inviteId);
        audit("TELEGRAM_CLAIM_REJECT", inviteId.toString(), "");
    }

    @PostMapping("/invites/{inviteId}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_telegram:invites:manage')")
    public void revokeInvite(@PathVariable UUID inviteId) {
        service.revokeInvite(inviteId);
        audit("TELEGRAM_INVITE_REVOKE", inviteId.toString(), "");
    }

    @PatchMapping("/users/{telegramUserId}/status")
    @PreAuthorize("hasAuthority('SCOPE_telegram:users:manage')")
    public void setStatus(@PathVariable String telegramUserId, @Valid @RequestBody SetStatusRequest request) {
        service.setAccountStatus(telegramUserId, request.status());
        audit("TELEGRAM_ACCOUNT_STATUS", telegramUserId, "status=" + request.status());
    }

    private void audit(String action, String resourceId, String details) {
        auditService.log(currentUserService.getCurrentUser(), action, "TELEGRAM", resourceId, details);
    }

    public record SetStatusRequest(@NotNull TelegramAccountStatus status) {}
    public record InviteResponse(UUID inviteId, String inviteUrl, Instant expiresAt, int expiresInMinutes) {}
}
