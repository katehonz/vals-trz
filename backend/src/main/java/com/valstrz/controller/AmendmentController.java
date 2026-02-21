package com.valstrz.controller;

import com.valstrz.entity.personnel.Amendment;
import com.valstrz.repository.AmendmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/{employeeId}/amendments")
public class AmendmentController {

    private final AmendmentRepository repository;

    public AmendmentController(AmendmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Amendment> getByEmployee(@PathVariable String tenantId,
                                              @PathVariable String employeeId) {
        return repository.findByTenantIdAndEmployeeId(tenantId, employeeId);
    }

    @GetMapping("/by-employment/{employmentId}")
    public Iterable<Amendment> getByEmployment(@PathVariable String tenantId,
                                                @PathVariable String employmentId) {
        return repository.findByTenantIdAndEmploymentId(tenantId, employmentId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Amendment> getById(@PathVariable String id) {
        Optional<Amendment> amendment = repository.findById(id);
        return amendment.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Amendment> create(@PathVariable String tenantId,
                                             @PathVariable String employeeId,
                                             @RequestBody Amendment amendment) {
        amendment.setTenantId(tenantId);
        amendment.setEmployeeId(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(amendment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Amendment> update(@PathVariable String tenantId,
                                             @PathVariable String employeeId,
                                             @PathVariable String id,
                                             @RequestBody Amendment amendment) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        amendment.setId(id);
        amendment.setTenantId(tenantId);
        amendment.setEmployeeId(employeeId);
        return ResponseEntity.ok(repository.save(amendment));
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
