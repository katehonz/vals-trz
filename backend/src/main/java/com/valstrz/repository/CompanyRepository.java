package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.company.Company;

public interface CompanyRepository extends ArangoRepository<Company, String> {
    Iterable<Company> findByBulstat(String bulstat);
}
