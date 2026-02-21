package com.valstrz.controller;

import com.valstrz.service.EmployeeImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/employees/import")
public class EmployeeImportController {

    private final EmployeeImportService service;

    public EmployeeImportController(EmployeeImportService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> importEmployees(@PathVariable String tenantId,
                                                @RequestParam("file") MultipartFile file) {
        try {
            service.importEmployees(tenantId, file);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
