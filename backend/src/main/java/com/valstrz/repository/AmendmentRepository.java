package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.Amendment;

public interface AmendmentRepository extends ArangoRepository<Amendment, String> {
    Iterable<Amendment> findByTenantId(String tenantId);
    Iterable<Amendment> findByTenantIdAndEmployeeId(String tenantId, String employeeId);
    Iterable<Amendment> findByTenantIdAndEmploymentId(String tenantId, String employmentId);
}
