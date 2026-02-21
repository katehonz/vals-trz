package com.valstrz.controller;

import com.valstrz.entity.insurance.InsuranceContributions;
import com.valstrz.entity.insurance.InsuranceRates;
import com.valstrz.repository.InsuranceContributionsRepository;
import com.valstrz.repository.InsuranceRatesRepository;
import com.valstrz.service.InsuranceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/insurance")
public class InsuranceController {

    private final InsuranceRatesRepository ratesRepository;
    private final InsuranceContributionsRepository contributionsRepository;
    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceRatesRepository ratesRepository,
                               InsuranceContributionsRepository contributionsRepository,
                               InsuranceService insuranceService) {
        this.ratesRepository = ratesRepository;
        this.contributionsRepository = contributionsRepository;
        this.insuranceService = insuranceService;
    }

    @PostMapping("/import/mod")
    public ResponseEntity<Void> importMod(@PathVariable String tenantId,
                                          @RequestParam("year") int year,
                                          @RequestParam("file") MultipartFile file) {
        try {
            insuranceService.importMod(tenantId, year, file);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Rates ---

    @GetMapping("/rates")
    public Iterable<InsuranceRates> getRates(@PathVariable String tenantId,
                                              @RequestParam(required = false) Integer year) {
        if (year != null) {
            return ratesRepository.findByTenantIdAndYear(tenantId, year);
        }
        return ratesRepository.findAll();
    }

    @PostMapping("/rates")
    public ResponseEntity<InsuranceRates> createRates(@PathVariable String tenantId,
                                                       @RequestBody InsuranceRates rates) {
        rates.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ratesRepository.save(rates));
    }

    @PutMapping("/rates/{id}")
    public ResponseEntity<InsuranceRates> updateRates(@PathVariable String tenantId,
                                                       @PathVariable String id,
                                                       @RequestBody InsuranceRates rates) {
        if (!ratesRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        rates.setId(id);
        rates.setTenantId(tenantId);
        return ResponseEntity.ok(ratesRepository.save(rates));
    }

    // --- Contributions ---

    @GetMapping("/contributions")
    public Iterable<InsuranceContributions> getContributions(@PathVariable String tenantId,
                                                              @RequestParam(required = false) Integer year) {
        if (year != null) {
            return contributionsRepository.findByTenantIdAndYear(tenantId, year);
        }
        return contributionsRepository.findAll();
    }

    @PostMapping("/contributions/seed")
    public List<InsuranceContributions> seedContributions(@PathVariable String tenantId,
                                                            @RequestParam(defaultValue = "2026") int year) {
        return insuranceService.seedContributions(tenantId, year);
    }

    @PostMapping("/contributions")
    public ResponseEntity<InsuranceContributions> createContributions(
            @PathVariable String tenantId,
            @RequestBody InsuranceContributions contributions) {
        contributions.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(contributionsRepository.save(contributions));
    }

    @PutMapping("/contributions/{id}")
    public ResponseEntity<InsuranceContributions> updateContributions(
            @PathVariable String tenantId,
            @PathVariable String id,
            @RequestBody InsuranceContributions contributions) {
        if (!contributionsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        contributions.setId(id);
        contributions.setTenantId(tenantId);
        return ResponseEntity.ok(contributionsRepository.save(contributions));
    }
}
