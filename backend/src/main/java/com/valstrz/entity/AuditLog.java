package com.valstrz.entity;

import com.arangodb.springframework.annotation.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document("auditLogs")
public class AuditLog extends BaseEntity {
    private String action;        // MONTH_CLOSE, MONTH_REOPEN, PAYROLL_CALCULATE, EMPLOYEE_IMPORT, etc.
    private String entityType;    // Payroll, Employee, MonthClosingSnapshot, etc.
    private String entityId;
    private String description;
    private Map<String, Object> details;
    private String performedBy;   // future: user ID
    private LocalDateTime performedAt;

    // Getters and setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}
