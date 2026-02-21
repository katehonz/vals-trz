package com.valstrz.controller;

import com.valstrz.entity.declaration.NapSubmission;
import com.valstrz.repository.NapSubmissionRepository;
import com.valstrz.service.Art73DeclarationService;
import com.valstrz.service.Article62Service;
import com.valstrz.service.Article123Service;
import com.valstrz.service.Declaration1Service;
import com.valstrz.service.Declaration6Service;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/declarations")
public class DeclarationController {

    private final Declaration1Service d1Service;
    private final Declaration6Service d6Service;
    private final Article62Service art62Service;
    private final Article123Service art123Service;
    private final Art73DeclarationService art73Service;
    private final NapSubmissionRepository submissionRepo;

    public DeclarationController(Declaration1Service d1Service,
                                  Declaration6Service d6Service,
                                  Article62Service art62Service,
                                  Article123Service art123Service,
                                  Art73DeclarationService art73Service,
                                  NapSubmissionRepository submissionRepo) {
        this.d1Service = d1Service;
        this.d6Service = d6Service;
        this.art62Service = art62Service;
        this.art123Service = art123Service;
        this.art73Service = art73Service;
        this.submissionRepo = submissionRepo;
    }

    // ── Декларация 1 ──

    @GetMapping("/d1/preview")
    public List<Declaration1Service.D1Record> d1Preview(@PathVariable String tenantId,
                                                         @RequestParam int year,
                                                         @RequestParam int month) {
        return d1Service.preview(tenantId, year, month);
    }

    @PostMapping("/d1/generate")
    public ResponseEntity<NapSubmission> d1Generate(@PathVariable String tenantId,
                                                      @RequestParam int year,
                                                      @RequestParam int month,
                                                      @RequestParam(defaultValue = "0") int correctionCode) {
        return ResponseEntity.ok(d1Service.generate(tenantId, year, month, correctionCode));
    }

    @GetMapping("/d1/validate")
    public List<Declaration1Service.ValidationError> d1Validate(@PathVariable String tenantId,
                                                                  @RequestParam int year,
                                                                  @RequestParam int month) {
        return d1Service.validate(tenantId, year, month);
    }

    // ── Декларация 6 ──

    @GetMapping("/d6/preview")
    public ResponseEntity<Declaration6Service.D6Data> d6Preview(@PathVariable String tenantId,
                                                                  @RequestParam int year,
                                                                  @RequestParam int month) {
        Declaration6Service.D6Data data = d6Service.preview(tenantId, year, month);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/d6/generate")
    public ResponseEntity<NapSubmission> d6Generate(@PathVariable String tenantId,
                                                      @RequestParam int year,
                                                      @RequestParam int month) {
        return ResponseEntity.ok(d6Service.generate(tenantId, year, month));
    }

    // ── Чл. 62 ──

    @GetMapping("/art62/preview")
    public List<Article62Service.Art62Record> art62Preview(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return art62Service.preview(tenantId, fromDate, toDate);
    }

    @PostMapping("/art62/generate")
    public ResponseEntity<NapSubmission> art62Generate(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(art62Service.generate(tenantId, fromDate, toDate));
    }

    // ── Чл. 123 ──

    @PostMapping("/art123/preview")
    public List<Article123Service.Art123Record> art123Preview(
            @PathVariable String tenantId,
            @RequestBody Article123Service.Art123Request request) {
        return art123Service.preview(tenantId, request);
    }

    @PostMapping("/art123/generate")
    public ResponseEntity<NapSubmission> art123Generate(
            @PathVariable String tenantId,
            @RequestBody Article123Service.Art123Request request) {
        return ResponseEntity.ok(art123Service.generate(tenantId, request));
    }

    // ── Чл. 73 ЗДДФЛ (годишна справка) ──

    @GetMapping("/art73/preview")
    public ResponseEntity<Art73DeclarationService.Art73Summary> art73Preview(
            @PathVariable String tenantId,
            @RequestParam int year) {
        return ResponseEntity.ok(art73Service.preview(tenantId, year));
    }

    @PostMapping("/art73/generate")
    public ResponseEntity<NapSubmission> art73Generate(
            @PathVariable String tenantId,
            @RequestParam int year) {
        return ResponseEntity.ok(art73Service.generate(tenantId, year));
    }

    // ── Submissions (одит) ──

    @GetMapping("/submissions")
    public List<NapSubmission> getSubmissions(@PathVariable String tenantId,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Integer year,
                                               @RequestParam(required = false) Integer month) {
        if (type != null && year != null && month != null) {
            return StreamSupport.stream(
                    submissionRepo.findByTenantIdAndTypeAndYearAndMonth(tenantId, type, year, month).spliterator(), false
            ).toList();
        }
        if (year != null && month != null) {
            return StreamSupport.stream(
                    submissionRepo.findByTenantIdAndYearAndMonth(tenantId, year, month).spliterator(), false
            ).toList();
        }
        return StreamSupport.stream(
                submissionRepo.findByTenantId(tenantId).spliterator(), false
        ).toList();
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<NapSubmission> getSubmission(@PathVariable String tenantId,
                                                         @PathVariable String id) {
        return submissionRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/submissions/{id}/download")
    public ResponseEntity<byte[]> downloadSubmission(@PathVariable String tenantId,
                                                       @PathVariable String id) {
        return submissionRepo.findById(id).map(sub -> {
            Charset win1251 = Charset.forName("windows-1251");
            byte[] bytes = sub.getFileContent().getBytes(win1251);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sub.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType("text/plain; charset=windows-1251"))
                    .contentLength(bytes.length)
                    .body(bytes);
        }).orElse(ResponseEntity.notFound().build());
    }
}
