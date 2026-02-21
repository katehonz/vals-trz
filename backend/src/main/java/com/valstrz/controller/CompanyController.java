package com.valstrz.controller;

import com.valstrz.entity.company.Company;
import com.valstrz.repository.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER','VIEWER')")
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public Iterable<Company> getAll() {
        return companyRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getById(@PathVariable String id) {
        Optional<Company> company = companyRepository.findById(id);
        return company.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody Company company) {
        Company saved = companyRepository.save(company);
        // tenantId на Company е собственият id
        saved.setTenantId(saved.getId());
        companyRepository.save(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> update(@PathVariable String id, @RequestBody Company company) {
        if (!companyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        company.setId(id);
        company.setTenantId(id);
        return ResponseEntity.ok(companyRepository.save(company));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!companyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        companyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
