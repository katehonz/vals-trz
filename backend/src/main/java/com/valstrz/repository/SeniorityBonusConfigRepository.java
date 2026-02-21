package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.company.SeniorityBonusConfig;

public interface SeniorityBonusConfigRepository extends ArangoRepository<SeniorityBonusConfig, String> {
    Iterable<SeniorityBonusConfig> findByTenantId(String tenantId);
}
