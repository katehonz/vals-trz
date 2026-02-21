package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.MonthlyTimesheet;

public interface MonthlyTimesheetRepository extends ArangoRepository<MonthlyTimesheet, String> {
    Iterable<MonthlyTimesheet> findByTenantIdAndEmployeeIdAndYearAndMonth(
            String tenantId, String employeeId, int year, int month);
    Iterable<MonthlyTimesheet> findByTenantIdAndYearAndMonth(
            String tenantId, int year, int month);
}
