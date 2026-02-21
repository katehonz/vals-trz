package com.valstrz.controller;

import com.valstrz.entity.insurance.InsuranceThreshold;
import com.valstrz.repository.InsuranceThresholdRepository;
import com.valstrz.service.InsuranceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/insurance-thresholds")
public class InsuranceThresholdController {

    private final InsuranceThresholdRepository repository;
    private final InsuranceService insuranceService;

    public InsuranceThresholdController(InsuranceThresholdRepository repository, InsuranceService insuranceService) {
        this.repository = repository;
        this.insuranceService = insuranceService;
    }

    @GetMapping
    public List<InsuranceThreshold> getAll(@PathVariable String tenantId,
                                           @RequestParam int year,
                                           @RequestParam(required = false) String nkidCode,
                                           @RequestParam(required = false) Integer personnelGroup) {
        Iterable<InsuranceThreshold> thresholds;
        if (nkidCode != null && personnelGroup != null) {
            thresholds = repository.findByTenantIdAndYearAndNkidCodeAndPersonnelGroup(tenantId, year, nkidCode, personnelGroup);
        } else if (personnelGroup != null) {
            thresholds = repository.findByTenantIdAndYearAndPersonnelGroup(tenantId, year, personnelGroup);
        } else {
            thresholds = repository.findByTenantIdAndYear(tenantId, year);
        }
        return StreamSupport.stream(thresholds.spliterator(), false).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceThreshold> getById(@PathVariable String tenantId, @PathVariable String id) {
        return repository.findById(id)
                .filter(t -> t.getTenantId().equals(tenantId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public InsuranceThreshold create(@PathVariable String tenantId, @RequestBody InsuranceThreshold threshold) {
        threshold.setTenantId(tenantId);
        return repository.save(threshold);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsuranceThreshold> update(@PathVariable String tenantId,
                                                    @PathVariable String id,
                                                    @RequestBody InsuranceThreshold threshold) {
        return repository.findById(id)
                .filter(t -> t.getTenantId().equals(tenantId))
                .map(existing -> {
                    threshold.setId(id);
                    threshold.setTenantId(tenantId);
                    return ResponseEntity.ok(repository.save(threshold));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String tenantId, @PathVariable String id) {
        Optional<InsuranceThreshold> existing = repository.findById(id);
        if (existing.isPresent() && existing.get().getTenantId().equals(tenantId)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/seed")
    public List<InsuranceThreshold> seed(@PathVariable String tenantId, @RequestParam int year) {
        return insuranceService.seedInsuranceThresholds(tenantId, year);
    }
}
