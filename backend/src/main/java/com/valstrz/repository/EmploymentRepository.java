package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.Employment;

public interface EmploymentRepository extends ArangoRepository<Employment, String> {
    Iterable<Employment> findByTenantId(String tenantId);
    Iterable<Employment> findByTenantIdAndEmployeeId(String tenantId, String employeeId);
    Iterable<Employment> findByTenantIdAndEmployeeIdAndCurrent(String tenantId, String employeeId, boolean current);
}
