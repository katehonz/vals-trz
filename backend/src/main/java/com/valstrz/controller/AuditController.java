package com.valstrz.controller;

import com.valstrz.entity.AuditLog;
import com.valstrz.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/companies/{tenantId}/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Iterable<AuditLog> getAll(@PathVariable String tenantId,
                                      @RequestParam(required = false) String action) {
        if (action != null) {
            return auditService.getByAction(tenantId, action);
        }
        return auditService.getByTenant(tenantId);
    }
}
