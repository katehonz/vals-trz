package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.AuditLog;

public interface AuditLogRepository extends ArangoRepository<AuditLog, String> {
    Iterable<AuditLog> findByTenantId(String tenantId);
    Iterable<AuditLog> findByTenantIdAndAction(String tenantId, String action);
    Iterable<AuditLog> findByTenantIdAndEntityType(String tenantId, String entityType);
}
