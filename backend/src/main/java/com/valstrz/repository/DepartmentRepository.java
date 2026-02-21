package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.structure.Department;

public interface DepartmentRepository extends ArangoRepository<Department, String> {
    Iterable<Department> findByTenantId(String tenantId);
    Iterable<Department> findByTenantIdAndParentId(String tenantId, String parentId);
}
