package com.spendwise.controller;

import com.spendwise.dto.service.AuditService;
import com.spendwise.dto.response.AuditLogResponse;
import com.spendwise.service.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
@Slf4j
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public AuditController(AuditService auditService, CurrentUserService currentUserService) {
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns the audit log entries recorded for the authenticated user.
     *
     * @return the user's audit history in reverse chronological order as provided by the service layer
     */
    @GetMapping
    public List<AuditLogResponse> list() {
        var user = currentUserService.getCurrentUser();
        log.info("Listing audit records for userId={}", user.getId());
        return auditService.list(user).stream().map(AuditLogResponse::from).toList();
    }
}
