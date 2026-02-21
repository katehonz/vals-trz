package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.EmployeeDeduction;

public interface EmployeeDeductionRepository extends ArangoRepository<EmployeeDeduction, String> {
    Iterable<EmployeeDeduction> findByTenantIdAndEmployeeId(String tenantId, String employeeId);
}
