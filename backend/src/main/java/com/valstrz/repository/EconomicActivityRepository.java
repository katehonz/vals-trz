package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.nomenclature.EconomicActivity;

public interface EconomicActivityRepository extends ArangoRepository<EconomicActivity, String> {
    Iterable<EconomicActivity> findByTenantIdAndYear(String tenantId, int year);
    Iterable<EconomicActivity> findByTenantIdAndYearAndActive(String tenantId, int year, boolean active);
    Iterable<EconomicActivity> findByTenantId(String tenantId);
}
