package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.calendar.WorkSchedule;

public interface WorkScheduleRepository extends ArangoRepository<WorkSchedule, String> {
    Iterable<WorkSchedule> findByTenantId(String tenantId);
}
