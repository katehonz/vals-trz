package com.valstrz.controller;

import com.valstrz.entity.nomenclature.EconomicActivity;
import com.valstrz.repository.EconomicActivityRepository;
import com.valstrz.service.InsuranceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/economic-activities")
public class EconomicActivityController {

    private final EconomicActivityRepository repository;
    private final InsuranceService insuranceService;

    public EconomicActivityController(EconomicActivityRepository repository,
                                       InsuranceService insuranceService) {
        this.repository = repository;
        this.insuranceService = insuranceService;
    }

    @GetMapping
    public Iterable<EconomicActivity> getAll(@PathVariable String tenantId,
                                              @RequestParam(required = false) Integer year,
                                              @RequestParam(required = false) Boolean active) {
        if (year != null && active != null) {
            return repository.findByTenantIdAndYearAndActive(tenantId, year, active);
        }
        if (year != null) {
            return repository.findByTenantIdAndYear(tenantId, year);
        }
        return repository.findByTenantId(tenantId);
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<EconomicActivity> toggleActive(@PathVariable String id) {
        Optional<EconomicActivity> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        EconomicActivity ea = opt.get();
        ea.setActive(!ea.isActive());
        return ResponseEntity.ok(repository.save(ea));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EconomicActivity> getById(@PathVariable String id) {
        Optional<EconomicActivity> item = repository.findById(id);
        return item.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EconomicActivity> create(@PathVariable String tenantId,
                                                     @RequestBody EconomicActivity activity) {
        activity.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(activity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EconomicActivity> update(@PathVariable String tenantId,
                                                     @PathVariable String id,
                                                     @RequestBody EconomicActivity activity) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        activity.setId(id);
        activity.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(activity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seed")
    public List<EconomicActivity> seed(@PathVariable String tenantId,
                                        @RequestParam(defaultValue = "2026") int year) {
        return insuranceService.seedEconomicActivities(tenantId, year);
    }
}
