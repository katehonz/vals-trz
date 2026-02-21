package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.Termination;

public interface TerminationRepository extends ArangoRepository<Termination, String> {
    Iterable<Termination> findByTenantId(String tenantId);
    Iterable<Termination> findByTenantIdAndEmployeeId(String tenantId, String employeeId);
    Iterable<Termination> findByTenantIdAndEmploymentId(String tenantId, String employmentId);
}
