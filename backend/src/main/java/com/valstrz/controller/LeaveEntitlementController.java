package com.valstrz.controller;

import com.valstrz.entity.personnel.LeaveEntitlement;
import com.valstrz.repository.LeaveEntitlementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/{employeeId}/leave-entitlements")
public class LeaveEntitlementController {

    private final LeaveEntitlementRepository repository;

    public LeaveEntitlementController(LeaveEntitlementRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<LeaveEntitlement> getByYear(@PathVariable String tenantId,
                                                  @PathVariable String employeeId,
                                                  @RequestParam int year) {
        return repository.findByTenantIdAndEmployeeIdAndYear(tenantId, employeeId, year);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveEntitlement> getById(@PathVariable String id) {
        Optional<LeaveEntitlement> le = repository.findById(id);
        return le.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LeaveEntitlement> create(@PathVariable String tenantId,
                                                     @PathVariable String employeeId,
                                                     @RequestBody LeaveEntitlement entitlement) {
        entitlement.setTenantId(tenantId);
        entitlement.setEmployeeId(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(entitlement));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveEntitlement> update(@PathVariable String tenantId,
                                                     @PathVariable String employeeId,
                                                     @PathVariable String id,
                                                     @RequestBody LeaveEntitlement entitlement) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entitlement.setId(id);
        entitlement.setTenantId(tenantId);
        entitlement.setEmployeeId(employeeId);
        return ResponseEntity.ok(repository.save(entitlement));
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
