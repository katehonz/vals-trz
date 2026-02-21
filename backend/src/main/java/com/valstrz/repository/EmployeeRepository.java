package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.Employee;

public interface EmployeeRepository extends ArangoRepository<Employee, String> {
    Iterable<Employee> findByTenantId(String tenantId);
    Iterable<Employee> findByTenantIdAndActive(String tenantId, boolean active);
    Iterable<Employee> findByTenantIdAndEgn(String tenantId, String egn);
    Iterable<Employee> findByTenantIdAndLastNameStartsWith(String tenantId, String prefix);
}
