package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.payroll.Payroll;

public interface PayrollRepository extends ArangoRepository<Payroll, String> {
    Iterable<Payroll> findByTenantId(String tenantId);
    Iterable<Payroll> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
    Iterable<Payroll> findByTenantIdAndStatus(String tenantId, String status);
}
