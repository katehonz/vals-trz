package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.company.PersonnelType;

public interface PersonnelTypeRepository extends ArangoRepository<PersonnelType, String> {
    Iterable<PersonnelType> findByTenantId(String tenantId);
}
