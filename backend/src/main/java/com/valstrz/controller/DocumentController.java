package com.valstrz.controller;

import com.valstrz.entity.document.DocumentTemplate;
import com.valstrz.repository.DocumentTemplateRepository;
import com.valstrz.service.CertificateService;
import com.valstrz.service.DocumentService;
import com.valstrz.service.TemplateSubstitutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/documents")
public class DocumentController {

    private final DocumentTemplateRepository templateRepo;
    private final DocumentService documentService;
    private final TemplateSubstitutionService substitutionService;
    private final CertificateService certificateService;

    public DocumentController(DocumentTemplateRepository templateRepo,
                               DocumentService documentService,
                               TemplateSubstitutionService substitutionService,
                               CertificateService certificateService) {
        this.templateRepo = templateRepo;
        this.documentService = documentService;
        this.substitutionService = substitutionService;
        this.certificateService = certificateService;
    }

    // ---- Template CRUD ----

    @GetMapping("/templates")
    public List<DocumentTemplate> getTemplates(@PathVariable String tenantId,
                                                @RequestParam(required = false) String category) {
        Iterable<DocumentTemplate> result = (category != null && !category.isEmpty())
                ? templateRepo.findByTenantIdAndCategory(tenantId, category)
                : templateRepo.findByTenantId(tenantId);
        return StreamSupport.stream(result.spliterator(), false).toList();
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<DocumentTemplate> getTemplate(@PathVariable String tenantId,
                                                         @PathVariable String id) {
        return templateRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/templates")
    public ResponseEntity<DocumentTemplate> createTemplate(@PathVariable String tenantId,
                                                            @RequestBody DocumentTemplate template) {
        template.setTenantId(tenantId);
        template.setSystem(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(templateRepo.save(template));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<DocumentTemplate> updateTemplate(@PathVariable String tenantId,
                                                            @PathVariable String id,
                                                            @RequestBody DocumentTemplate template) {
        return templateRepo.findById(id).map(existing -> {
            template.setId(id);
            template.setTenantId(tenantId);
            template.setSystem(existing.isSystem());
            return ResponseEntity.ok(templateRepo.save(template));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String tenantId,
                                                @PathVariable String id) {
        return templateRepo.findById(id).map(t -> {
            if (t.isSystem()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).<Void>build();
            }
            templateRepo.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ---- Seed ----

    @PostMapping("/templates/seed")
    public List<DocumentTemplate> seedTemplates(@PathVariable String tenantId) {
        return documentService.seedSystemTemplates(tenantId);
    }

    // ---- Generate ----

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(@PathVariable String tenantId,
                                                         @RequestBody GenerateRequest request) {
        String html = documentService.generateDocument(
                tenantId, request.templateId(), request.employeeId(), request.contextId());
        return ResponseEntity.ok(Map.of("html", html));
    }

    // ---- Placeholders ----

    @GetMapping("/placeholders")
    public Map<String, List<String>> getPlaceholders(@RequestParam(required = false) String category) {
        return substitutionService.getAvailablePlaceholders(category);
    }

    // ---- УП-2 ----

    @PostMapping("/generate-up2")
    public ResponseEntity<CertificateService.UP2Data> generateUP2(@PathVariable String tenantId,
                                                                     @RequestBody UP2Request request) {
        return ResponseEntity.ok(certificateService.generateUP2(
                tenantId, request.employeeId(),
                request.fromYear(), request.fromMonth(),
                request.toYear(), request.toMonth()));
    }

    // ---- УП-3 ----

    @PostMapping("/generate-up3")
    public ResponseEntity<CertificateService.UP3Data> generateUP3(@PathVariable String tenantId,
                                                                     @RequestBody UP3Request request) {
        return ResponseEntity.ok(certificateService.generateUP3(
                tenantId, request.employeeId(),
                java.time.LocalDate.parse(request.fromDate()),
                java.time.LocalDate.parse(request.toDate())));
    }

    // ---- Request DTOs ----

    record GenerateRequest(String templateId, String employeeId, String contextId) {}

    record UP2Request(String employeeId, int fromYear, int fromMonth, int toYear, int toMonth) {}

    record UP3Request(String employeeId, String fromDate, String toDate) {}
}
