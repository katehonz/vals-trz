package com.valstrz.controller;

import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.payroll.PayrollSnapshot.PayrollLine;
import com.valstrz.service.PayrollService;
import com.valstrz.service.PdfExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/export")
public class ExportController {

    private final PayrollService payrollService;
    private final PdfExportService pdfExportService;

    public ExportController(PayrollService payrollService, PdfExportService pdfExportService) {
        this.payrollService = payrollService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/payroll/csv")
    public ResponseEntity<byte[]> exportPayrollCsv(@PathVariable String tenantId,
                                                     @RequestParam int year,
                                                     @RequestParam int month) {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);

        StringBuilder csv = new StringBuilder();
        // BOM for Excel UTF-8 detection
        csv.append('\uFEFF');
        // Header
        csv.append("Служител,ЕГН,Длъжност,Брутно,Осиг. доход,Осиг. работник,Данъчна основа,ДОД,Нето,Осиг. работодател,Цена на труда\n");

        for (PayrollSnapshot s : snapshots) {
            StringJoiner line = new StringJoiner(",");
            line.add(csvEscape(s.getEmployeeData() != null ? String.valueOf(s.getEmployeeData().get("fullName")) : ""));
            line.add(csvEscape(s.getEmployeeData() != null ? String.valueOf(s.getEmployeeData().get("egn")) : ""));
            line.add(csvEscape(s.getEmployeeData() != null ? String.valueOf(s.getEmployeeData().get("jobTitle")) : ""));
            line.add(fmt(s.getGrossSalary()));
            line.add(fmt(s.getInsurableIncome()));
            line.add(fmt(s.getTotalEmployeeInsurance()));
            line.add(fmt(s.getTaxBase()));
            line.add(fmt(s.getIncomeTax()));
            line.add(fmt(s.getNetSalary()));
            line.add(fmt(s.getTotalEmployerInsurance()));
            line.add(fmt(s.getTotalEmployerCost()));
            csv.append(line).append('\n');
        }

        String filename = String.format("payroll_%d_%02d.csv", year, month);
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/employees/csv")
    public ResponseEntity<byte[]> exportEmployeesCsv(@PathVariable String tenantId) {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId,
                java.time.LocalDate.now().getYear(), java.time.LocalDate.now().getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("ЕГН,Име,Презиме,Фамилия,Длъжност,НКПД,Основна заплата\n");

        for (PayrollSnapshot s : snapshots) {
            if (s.getEmployeeData() == null) continue;
            StringJoiner line = new StringJoiner(",");
            line.add(csvEscape(String.valueOf(s.getEmployeeData().getOrDefault("egn", ""))));
            line.add(csvEscape(String.valueOf(s.getEmployeeData().getOrDefault("firstName", ""))));
            line.add(csvEscape(String.valueOf(s.getEmployeeData().getOrDefault("middleName", ""))));
            line.add(csvEscape(String.valueOf(s.getEmployeeData().getOrDefault("lastName", ""))));
            line.add(csvEscape(String.valueOf(s.getEmployeeData().getOrDefault("jobTitle", ""))));
            line.add(csvEscape(String.valueOf(s.getEmployeeData().getOrDefault("nkpdCode", ""))));
            line.add(fmt(s.getGrossSalary()));
            csv.append(line).append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/payroll/excel")
    public ResponseEntity<byte[]> exportPayrollExcel(@PathVariable String tenantId,
                                                       @RequestParam int year,
                                                       @RequestParam int month) throws IOException {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ведомост " + month + "/" + year);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            // Header row
            String[] headers = {"Служител", "ЕГН", "Длъжност", "Брутно", "Осиг. доход",
                    "Осиг. работник", "Данъчна основа", "ДОД", "Нето", "Осиг. работодател", "Цена на труда"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (PayrollSnapshot s : snapshots) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getEmployeeData() != null ? String.valueOf(s.getEmployeeData().getOrDefault("fullName", "")) : "");
                row.createCell(1).setCellValue(s.getEmployeeData() != null ? String.valueOf(s.getEmployeeData().getOrDefault("egn", "")) : "");
                row.createCell(2).setCellValue(s.getEmployeeData() != null ? String.valueOf(s.getEmployeeData().getOrDefault("jobTitle", "")) : "");
                setMoney(row.createCell(3), s.getGrossSalary(), moneyStyle);
                setMoney(row.createCell(4), s.getInsurableIncome(), moneyStyle);
                setMoney(row.createCell(5), s.getTotalEmployeeInsurance(), moneyStyle);
                setMoney(row.createCell(6), s.getTaxBase(), moneyStyle);
                setMoney(row.createCell(7), s.getIncomeTax(), moneyStyle);
                setMoney(row.createCell(8), s.getNetSalary(), moneyStyle);
                setMoney(row.createCell(9), s.getTotalEmployerInsurance(), moneyStyle);
                setMoney(row.createCell(10), s.getTotalEmployerCost(), moneyStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            String filename = String.format("payroll_%d_%02d.xlsx", year, month);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        }
    }

    @GetMapping("/payroll/pdf")
    public ResponseEntity<byte[]> exportPayrollPdf(@PathVariable String tenantId,
                                                      @RequestParam int year,
                                                      @RequestParam int month) throws Exception {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);
        if (snapshots.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        byte[] pdf = pdfExportService.generateAllSlips(snapshots, tenantId);
        String filename = String.format("payroll_%d_%02d.pdf", year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/payroll/pdf/{employeeId}")
    public ResponseEntity<byte[]> exportEmployeePdf(@PathVariable String tenantId,
                                                       @PathVariable String employeeId,
                                                       @RequestParam int year,
                                                       @RequestParam int month) throws Exception {
        PayrollSnapshot snapshot = payrollService.getEmployeeSnapshot(tenantId, employeeId, year, month);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdf = pdfExportService.generateSalarySlip(snapshot, tenantId);
        String filename = String.format("slip_%s_%d_%02d.pdf", employeeId, year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void setMoney(Cell cell, java.math.BigDecimal val, CellStyle style) {
        cell.setCellValue(val != null ? val.doubleValue() : 0.00);
        cell.setCellStyle(style);
    }

    private String fmt(java.math.BigDecimal val) {
        return val != null ? val.toPlainString() : "0.00";
    }

    private String csvEscape(String value) {
        if (value == null || "null".equals(value)) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
