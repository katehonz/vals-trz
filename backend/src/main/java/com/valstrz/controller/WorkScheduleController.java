package com.valstrz.controller;

import com.valstrz.entity.calendar.WorkSchedule;
import com.valstrz.repository.WorkScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/work-schedules")
public class WorkScheduleController {

    private final WorkScheduleRepository repository;

    public WorkScheduleController(WorkScheduleRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<WorkSchedule> getAll(@PathVariable String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @PostMapping
    public ResponseEntity<WorkSchedule> create(@PathVariable String tenantId,
                                                @RequestBody WorkSchedule schedule) {
        schedule.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkSchedule> update(@PathVariable String tenantId,
                                                @PathVariable String id,
                                                @RequestBody WorkSchedule schedule) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        schedule.setId(id);
        schedule.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
