package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.calendar.ShiftSchedule;

public interface ShiftScheduleRepository extends ArangoRepository<ShiftSchedule, String> {
    Iterable<ShiftSchedule> findByTenantId(String tenantId);
    Iterable<ShiftSchedule> findByTenantIdAndActive(String tenantId, boolean active);
}
