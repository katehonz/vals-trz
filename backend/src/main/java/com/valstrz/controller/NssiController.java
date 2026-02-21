package com.valstrz.controller;

import com.valstrz.entity.personnel.Absence;
import com.valstrz.repository.AbsenceRepository;
import com.valstrz.service.NssiExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/nssi")
public class NssiController {

    private final AbsenceRepository absenceRepository;
    private final NssiExportService nssiExportService;

    public NssiController(AbsenceRepository absenceRepository, NssiExportService nssiExportService) {
        this.absenceRepository = absenceRepository;
        this.nssiExportService = nssiExportService;
    }

    @GetMapping("/export/pril9")
    public ResponseEntity<byte[]> exportPril9(@PathVariable String tenantId,
                                               @RequestParam(required = false) String employeeId) {
        
        Iterable<Absence> allAbsences;
        if (employeeId != null) {
            allAbsences = absenceRepository.findByTenantIdAndEmployeeId(tenantId, employeeId);
        } else {
            allAbsences = absenceRepository.findAll();
        }

        List<Absence> sickLeaves = StreamSupport.stream(allAbsences.spliterator(), false)
                .filter(a -> a.getType() != null && a.getType().startsWith("SICK"))
                .filter(a -> a.getTenantId().equals(tenantId))
                .collect(Collectors.toList());

        if (sickLeaves.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        String xml = nssiExportService.generateAnnex9Xml(tenantId, sickLeaves);
        byte[] content = xml.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Pril9_" + tenantId + ".xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(content);
    }
}
