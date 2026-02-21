package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.LeaveEntitlement;

public interface LeaveEntitlementRepository extends ArangoRepository<LeaveEntitlement, String> {
    Iterable<LeaveEntitlement> findByTenantIdAndEmployeeIdAndYear(String tenantId, String employeeId, int year);
}
