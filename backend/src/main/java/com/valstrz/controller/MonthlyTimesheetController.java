package com.valstrz.controller;

import com.valstrz.entity.personnel.MonthlyTimesheet;
import com.valstrz.repository.MonthlyTimesheetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/timesheets")
public class MonthlyTimesheetController {

    private final MonthlyTimesheetRepository repository;

    public MonthlyTimesheetController(MonthlyTimesheetRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<MonthlyTimesheet> getByMonth(@PathVariable String tenantId,
                                                   @RequestParam int year,
                                                   @RequestParam int month) {
        return repository.findByTenantIdAndYearAndMonth(tenantId, year, month);
    }

    @GetMapping("/employee/{employeeId}")
    public Iterable<MonthlyTimesheet> getByEmployee(@PathVariable String tenantId,
                                                      @PathVariable String employeeId,
                                                      @RequestParam int year,
                                                      @RequestParam int month) {
        return repository.findByTenantIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, year, month);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthlyTimesheet> getById(@PathVariable String id) {
        Optional<MonthlyTimesheet> ts = repository.findById(id);
        return ts.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MonthlyTimesheet> create(@PathVariable String tenantId,
                                                     @RequestBody MonthlyTimesheet timesheet) {
        timesheet.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(timesheet));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonthlyTimesheet> update(@PathVariable String tenantId,
                                                     @PathVariable String id,
                                                     @RequestBody MonthlyTimesheet timesheet) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        timesheet.setId(id);
        timesheet.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(timesheet));
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
