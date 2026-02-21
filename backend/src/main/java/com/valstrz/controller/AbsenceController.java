package com.valstrz.controller;

import com.valstrz.entity.personnel.Absence;
import com.valstrz.repository.AbsenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/{employeeId}/absences")
public class AbsenceController {

    private final AbsenceRepository repository;

    public AbsenceController(AbsenceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Absence> getByEmployee(@PathVariable String tenantId,
                                            @PathVariable String employeeId) {
        return repository.findByTenantIdAndEmployeeId(tenantId, employeeId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Absence> getById(@PathVariable String id) {
        Optional<Absence> absence = repository.findById(id);
        return absence.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Absence> create(@PathVariable String tenantId,
                                           @PathVariable String employeeId,
                                           @RequestBody Absence absence) {
        absence.setTenantId(tenantId);
        absence.setEmployeeId(employeeId);
        calculateDays(absence);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(absence));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Absence> update(@PathVariable String tenantId,
                                           @PathVariable String employeeId,
                                           @PathVariable String id,
                                           @RequestBody Absence absence) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        absence.setId(id);
        absence.setTenantId(tenantId);
        absence.setEmployeeId(employeeId);
        calculateDays(absence);
        return ResponseEntity.ok(repository.save(absence));
    }

    private void calculateDays(Absence absence) {
        if (absence.getFromDate() == null || absence.getToDate() == null) return;

        int totalWorkingDays = 0;
        int employerDays = 0;
        
        java.time.LocalDate cur = absence.getFromDate();
        while (!cur.isAfter(absence.getToDate())) {
            // Simple check: Skip Saturday (6) and Sunday (7)
            if (cur.getDayOfWeek().getValue() < 6) {
                totalWorkingDays++;
                // If it's a sick leave, first 2 working days are for the employer
                if (absence.getType() != null && absence.getType().startsWith("SICK") && totalWorkingDays <= 2) {
                    employerDays++;
                }
            }
            cur = cur.plusDays(1);
        }

        absence.setWorkingDays(totalWorkingDays);
        if (absence.getType() != null && absence.getType().startsWith("SICK")) {
            absence.setEmployerDays(employerDays);
            absence.setNssiDays(totalWorkingDays - employerDays);
        } else {
            absence.setEmployerDays(0);
            absence.setNssiDays(0);
        }
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
