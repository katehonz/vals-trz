package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.Absence;

public interface AbsenceRepository extends ArangoRepository<Absence, String> {
    Iterable<Absence> findByTenantIdAndEmployeeId(String tenantId, String employeeId);
}
