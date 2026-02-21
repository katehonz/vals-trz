package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.declaration.NapSubmission;

public interface NapSubmissionRepository extends ArangoRepository<NapSubmission, String> {

    Iterable<NapSubmission> findByTenantIdAndTypeAndYearAndMonth(
            String tenantId, String type, int year, int month);

    Iterable<NapSubmission> findByTenantIdAndYearAndMonth(
            String tenantId, int year, int month);

    Iterable<NapSubmission> findByTenantId(String tenantId);
}
