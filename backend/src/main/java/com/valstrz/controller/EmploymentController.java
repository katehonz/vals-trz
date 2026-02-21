package com.valstrz.controller;

import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.EmploymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/{employeeId}/employments")
public class EmploymentController {

    private final EmploymentRepository repository;

    public EmploymentController(EmploymentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Employment> getAll(@PathVariable String tenantId,
                                        @PathVariable String employeeId,
                                        @RequestParam(required = false) Boolean current) {
        if (current != null) {
            return repository.findByTenantIdAndEmployeeIdAndCurrent(tenantId, employeeId, current);
        }
        return repository.findByTenantIdAndEmployeeId(tenantId, employeeId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employment> getById(@PathVariable String id) {
        Optional<Employment> employment = repository.findById(id);
        return employment.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Employment> create(@PathVariable String tenantId,
                                              @PathVariable String employeeId,
                                              @RequestBody Employment employment) {
        employment.setTenantId(tenantId);
        employment.setEmployeeId(employeeId);
        employment.setCurrent(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(employment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employment> update(@PathVariable String tenantId,
                                              @PathVariable String employeeId,
                                              @PathVariable String id,
                                              @RequestBody Employment employment) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employment.setId(id);
        employment.setTenantId(tenantId);
        employment.setEmployeeId(employeeId);
        return ResponseEntity.ok(repository.save(employment));
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
