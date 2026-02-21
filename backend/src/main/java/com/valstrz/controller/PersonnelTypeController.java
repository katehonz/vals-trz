package com.valstrz.controller;

import com.valstrz.entity.company.PersonnelType;
import com.valstrz.repository.PersonnelTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/personnel-types")
public class PersonnelTypeController {

    private final PersonnelTypeRepository repository;

    public PersonnelTypeController(PersonnelTypeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<PersonnelType> getAll(@PathVariable String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @PostMapping
    public ResponseEntity<PersonnelType> create(@PathVariable String tenantId,
                                                 @RequestBody PersonnelType type) {
        type.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(type));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonnelType> update(@PathVariable String tenantId,
                                                 @PathVariable String id,
                                                 @RequestBody PersonnelType type) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        type.setId(id);
        type.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(type));
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
