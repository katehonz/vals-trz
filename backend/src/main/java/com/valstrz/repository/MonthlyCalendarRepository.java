package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.calendar.MonthlyCalendar;

public interface MonthlyCalendarRepository extends ArangoRepository<MonthlyCalendar, String> {
    Iterable<MonthlyCalendar> findByTenantId(String tenantId);
    Iterable<MonthlyCalendar> findByTenantIdAndYear(String tenantId, int year);
    Iterable<MonthlyCalendar> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
}
