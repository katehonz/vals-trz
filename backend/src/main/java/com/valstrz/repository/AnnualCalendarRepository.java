package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.calendar.AnnualCalendar;

public interface AnnualCalendarRepository extends ArangoRepository<AnnualCalendar, String> {
    Iterable<AnnualCalendar> findByTenantId(String tenantId);
    Iterable<AnnualCalendar> findByTenantIdAndYear(String tenantId, int year);
}
