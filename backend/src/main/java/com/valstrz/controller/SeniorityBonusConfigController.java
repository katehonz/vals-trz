package com.valstrz.controller;

import com.valstrz.entity.company.SeniorityBonusConfig;
import com.valstrz.repository.SeniorityBonusConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/seniority-config")
public class SeniorityBonusConfigController {

    private final SeniorityBonusConfigRepository repository;

    public SeniorityBonusConfigController(SeniorityBonusConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<SeniorityBonusConfig> get(@PathVariable String tenantId) {
        for (SeniorityBonusConfig c : repository.findByTenantId(tenantId)) {
            return ResponseEntity.ok(c);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<SeniorityBonusConfig> createOrUpdate(@PathVariable String tenantId,
                                                                  @RequestBody SeniorityBonusConfig config) {
        // Ако вече съществува, обновяваме
        for (SeniorityBonusConfig existing : repository.findByTenantId(tenantId)) {
            existing.setPercentPerYear(config.getPercentPerYear());
            existing.setAutoUpdateOnMonthClose(config.isAutoUpdateOnMonthClose());
            existing.setBrackets(config.getBrackets());
            return ResponseEntity.ok(repository.save(existing));
        }
        // Нов запис
        config.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(config));
    }
}
