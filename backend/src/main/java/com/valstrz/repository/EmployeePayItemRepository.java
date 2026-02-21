package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.EmployeePayItem;

public interface EmployeePayItemRepository extends ArangoRepository<EmployeePayItem, String> {
    Iterable<EmployeePayItem> findByTenantIdAndEmployeeId(String tenantId, String employeeId);
}
