package com.valstrz.controller;

import com.valstrz.entity.structure.Department;
import com.valstrz.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/departments")
public class DepartmentController {

    private final DepartmentRepository repository;

    public DepartmentController(DepartmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Department> getAll(@PathVariable String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/by-parent/{parentId}")
    public Iterable<Department> getByParent(@PathVariable String tenantId,
                                             @PathVariable String parentId) {
        return repository.findByTenantIdAndParentId(tenantId, parentId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getById(@PathVariable String id) {
        Optional<Department> dept = repository.findById(id);
        return dept.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Department> create(@PathVariable String tenantId,
                                              @RequestBody Department department) {
        department.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(department));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable String tenantId,
                                              @PathVariable String id,
                                              @RequestBody Department department) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        department.setId(id);
        department.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(department));
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
