package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.insurance.InsuranceRates;

public interface InsuranceRatesRepository extends ArangoRepository<InsuranceRates, String> {
    Iterable<InsuranceRates> findByTenantIdAndYear(String tenantId, int year);
}
