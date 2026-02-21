package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.payroll.PayrollSnapshot;

public interface PayrollSnapshotRepository extends ArangoRepository<PayrollSnapshot, String> {
    Iterable<PayrollSnapshot> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
    Iterable<PayrollSnapshot> findByTenantIdAndEmployeeIdAndYearAndMonth(
            String tenantId, String employeeId, int year, int month);

    Iterable<PayrollSnapshot> findByTenantIdAndYearAndMonthAndStatus(
            String tenantId, int year, int month, String status);
}
