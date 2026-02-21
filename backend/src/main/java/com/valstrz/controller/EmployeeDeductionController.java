package com.valstrz.controller;

import com.valstrz.entity.personnel.EmployeeDeduction;
import com.valstrz.repository.EmployeeDeductionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/{employeeId}/deductions")
public class EmployeeDeductionController {

    private final EmployeeDeductionRepository repository;

    public EmployeeDeductionController(EmployeeDeductionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<EmployeeDeduction> getAll(@PathVariable String tenantId,
                                                @PathVariable String employeeId) {
        return repository.findByTenantIdAndEmployeeId(tenantId, employeeId);
    }

    @PostMapping
    public ResponseEntity<EmployeeDeduction> create(@PathVariable String tenantId,
                                                       @PathVariable String employeeId,
                                                       @RequestBody EmployeeDeduction item) {
        item.setTenantId(tenantId);
        item.setEmployeeId(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDeduction> update(@PathVariable String tenantId,
                                                       @PathVariable String employeeId,
                                                       @PathVariable String id,
                                                       @RequestBody EmployeeDeduction item) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        item.setId(id);
        item.setTenantId(tenantId);
        item.setEmployeeId(employeeId);
        return ResponseEntity.ok(repository.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
