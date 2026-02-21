package com.valstrz.controller;

import com.valstrz.entity.personnel.Employee;
import com.valstrz.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees")
public class EmployeeController {

    private final EmployeeRepository repository;

    public EmployeeController(EmployeeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Employee> getAll(@PathVariable String tenantId,
                                      @RequestParam(required = false) Boolean active) {
        if (active != null) {
            return repository.findByTenantIdAndActive(tenantId, active);
        }
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/search")
    public Iterable<Employee> search(@PathVariable String tenantId,
                                      @RequestParam(required = false) String egn,
                                      @RequestParam(required = false) String lastName) {
        if (egn != null) {
            return repository.findByTenantIdAndEgn(tenantId, egn);
        }
        if (lastName != null) {
            return repository.findByTenantIdAndLastNameStartsWith(tenantId, lastName);
        }
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(@PathVariable String id) {
        Optional<Employee> employee = repository.findById(id);
        return employee.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Employee> create(@PathVariable String tenantId,
                                            @RequestBody Employee employee) {
        employee.setTenantId(tenantId);
        employee.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable String tenantId,
                                            @PathVariable String id,
                                            @RequestBody Employee employee) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employee.setId(id);
        employee.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(employee));
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
