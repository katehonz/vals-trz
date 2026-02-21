package com.valstrz.service;

import com.valstrz.entity.AuditLog;
import com.valstrz.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public AuditLog log(String tenantId, String action, String entityType,
                         String entityId, String description, Map<String, Object> details) {
        AuditLog entry = new AuditLog();
        entry.setTenantId(tenantId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDescription(description);
        entry.setDetails(details);
        entry.setPerformedBy(getCurrentUsername());
        entry.setPerformedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "system";
    }

    public Iterable<AuditLog> getByTenant(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public Iterable<AuditLog> getByAction(String tenantId, String action) {
        return repository.findByTenantIdAndAction(tenantId, action);
    }
}
