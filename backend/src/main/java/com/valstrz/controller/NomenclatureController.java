package com.valstrz.controller;

import com.valstrz.entity.nomenclature.Nomenclature;
import com.valstrz.repository.NomenclatureRepository;
import com.valstrz.service.NomenclatureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/nomenclatures")
public class NomenclatureController {

    private final NomenclatureRepository repository;
    private final NomenclatureService service;

    public NomenclatureController(NomenclatureRepository repository, NomenclatureService service) {
        this.repository = repository;
        this.service = service;
    }

    @PostMapping("/import/nkpd")
    public ResponseEntity<Void> importNkpd(@PathVariable String tenantId,
                                           @RequestParam("file") MultipartFile file) {
        try {
            service.importNkpd(tenantId, file);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public Iterable<Nomenclature> getAll(@PathVariable String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<Nomenclature> getByCode(@PathVariable String tenantId,
                                                    @PathVariable String code) {
        var results = repository.findByTenantIdAndCode(tenantId, code);
        var iterator = results.iterator();
        if (iterator.hasNext()) {
            return ResponseEntity.ok(iterator.next());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Nomenclature> getById(@PathVariable String id) {
        Optional<Nomenclature> nom = repository.findById(id);
        return nom.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Nomenclature> create(@PathVariable String tenantId,
                                                @RequestBody Nomenclature nomenclature) {
        nomenclature.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(nomenclature));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Nomenclature> update(@PathVariable String tenantId,
                                                @PathVariable String id,
                                                @RequestBody Nomenclature nomenclature) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        nomenclature.setId(id);
        nomenclature.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(nomenclature));
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
