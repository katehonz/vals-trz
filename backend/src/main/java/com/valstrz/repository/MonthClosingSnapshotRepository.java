package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.payroll.MonthClosingSnapshot;

public interface MonthClosingSnapshotRepository extends ArangoRepository<MonthClosingSnapshot, String> {
    Iterable<MonthClosingSnapshot> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
}
