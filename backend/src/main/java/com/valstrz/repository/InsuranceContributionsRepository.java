package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.insurance.InsuranceContributions;

public interface InsuranceContributionsRepository extends ArangoRepository<InsuranceContributions, String> {
    Iterable<InsuranceContributions> findByTenantIdAndYear(String tenantId, int year);
    Iterable<InsuranceContributions> findByTenantIdAndYearAndCategory(String tenantId, int year, String category);
    Iterable<InsuranceContributions> findByTenantIdAndYearAndCategoryAndInsuredType(String tenantId, int year, String category, String insuredType);
}
