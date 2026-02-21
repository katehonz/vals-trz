package com.valstrz.controller;

import com.valstrz.entity.personnel.Termination;
import com.valstrz.repository.TerminationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/{employeeId}/terminations")
public class TerminationController {

    private final TerminationRepository repository;

    public TerminationController(TerminationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Termination> getByEmployee(@PathVariable String tenantId,
                                                @PathVariable String employeeId) {
        return repository.findByTenantIdAndEmployeeId(tenantId, employeeId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Termination> getById(@PathVariable String id) {
        Optional<Termination> termination = repository.findById(id);
        return termination.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Termination> create(@PathVariable String tenantId,
                                               @PathVariable String employeeId,
                                               @RequestBody Termination termination) {
        termination.setTenantId(tenantId);
        termination.setEmployeeId(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(termination));
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
