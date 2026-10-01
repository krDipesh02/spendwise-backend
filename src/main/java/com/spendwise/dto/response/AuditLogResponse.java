package com.spendwise.dto.response;

import com.spendwise.dto.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private String id;
    private String action;
    private String resourceType;
    private String resourceId;
    private String details;
    private Instant createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId().toString(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
