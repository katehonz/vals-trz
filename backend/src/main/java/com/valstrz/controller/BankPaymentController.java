package com.valstrz.controller;

import com.valstrz.service.BankPaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/bank-payments")
public class BankPaymentController {

    private final BankPaymentService bankPaymentService;

    public BankPaymentController(BankPaymentService bankPaymentService) {
        this.bankPaymentService = bankPaymentService;
    }

    @GetMapping("/preview")
    public List<BankPaymentService.PaymentRecord> preview(@PathVariable String tenantId,
                                                            @RequestParam int year,
                                                            @RequestParam int month) {
        return bankPaymentService.preview(tenantId, year, month);
    }

    @PostMapping("/generate")
    public ResponseEntity<BankPaymentService.PaymentFileResult> generate(@PathVariable String tenantId,
                                                                          @RequestParam int year,
                                                                          @RequestParam int month) {
        return ResponseEntity.ok(bankPaymentService.generateFile(tenantId, year, month));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@PathVariable String tenantId,
                                             @RequestParam int year,
                                             @RequestParam int month) {
        BankPaymentService.PaymentFileResult result = bankPaymentService.generateFile(tenantId, year, month);
        byte[] bytes = result.fileContent().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
