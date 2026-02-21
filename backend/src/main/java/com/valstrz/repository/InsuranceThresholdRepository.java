package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.insurance.InsuranceThreshold;

public interface InsuranceThresholdRepository extends ArangoRepository<InsuranceThreshold, String> {
    Iterable<InsuranceThreshold> findByTenantIdAndYear(String tenantId, int year);
    Iterable<InsuranceThreshold> findByTenantIdAndYearAndPersonnelGroup(
            String tenantId, int year, int personnelGroup);
    Iterable<InsuranceThreshold> findByTenantIdAndYearAndNkidCodeAndPersonnelGroup(
            String tenantId, int year, String nkidCode, int personnelGroup);
}
