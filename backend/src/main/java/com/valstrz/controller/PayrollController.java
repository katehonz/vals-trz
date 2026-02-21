package com.valstrz.controller;

import com.valstrz.entity.payroll.MonthClosingSnapshot;
import com.valstrz.entity.payroll.Payroll;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.service.MonthClosingService;
import com.valstrz.service.PayrollReportService;
import com.valstrz.service.PayrollService;
import com.valstrz.service.YearClosingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/payroll")
public class PayrollController {

    private final PayrollService payrollService;
    private final MonthClosingService monthClosingService;
    private final PayrollReportService reportService;
    private final YearClosingService yearClosingService;

    public PayrollController(PayrollService payrollService,
                              MonthClosingService monthClosingService,
                              PayrollReportService reportService,
                              YearClosingService yearClosingService) {
        this.payrollService = payrollService;
        this.monthClosingService = monthClosingService;
        this.reportService = reportService;
        this.yearClosingService = yearClosingService;
    }

    // ── Статус ──

    @GetMapping
    public ResponseEntity<Payroll> getPayroll(@PathVariable String tenantId,
                                               @RequestParam int year,
                                               @RequestParam int month) {
        return ResponseEntity.ok(payrollService.getOrCreatePayroll(tenantId, year, month));
    }

    @PostMapping("/start-new")
    public ResponseEntity<Void> startNewMonth(@PathVariable String tenantId,
                                              @RequestParam int year,
                                              @RequestParam int month) {
        monthClosingService.startNewMonth(tenantId, year, month);
        return ResponseEntity.ok().build();
    }

    // ── Изчисление ──

    @PostMapping("/calculate")
    public ResponseEntity<List<PayrollSnapshot>> calculateAll(@PathVariable String tenantId,
                                                                @RequestParam int year,
                                                                @RequestParam int month) {
        List<PayrollSnapshot> results = payrollService.calculateAll(tenantId, year, month);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/calculate/{employeeId}")
    public ResponseEntity<PayrollSnapshot> calculateEmployee(@PathVariable String tenantId,
                                                               @PathVariable String employeeId,
                                                               @RequestParam int year,
                                                               @RequestParam int month) {
        PayrollSnapshot snapshot = payrollService.calculateForEmployee(tenantId, employeeId, year, month);
        return ResponseEntity.ok(snapshot);
    }

    // ── Snapshots ──

    @GetMapping("/snapshots")
    public List<PayrollSnapshot> getSnapshots(@PathVariable String tenantId,
                                               @RequestParam int year,
                                               @RequestParam int month) {
        return payrollService.getPayrollSnapshots(tenantId, year, month);
    }

    @GetMapping("/snapshots/{employeeId}")
    public ResponseEntity<PayrollSnapshot> getEmployeeSnapshot(@PathVariable String tenantId,
                                                                 @PathVariable String employeeId,
                                                                 @RequestParam int year,
                                                                 @RequestParam int month) {
        PayrollSnapshot snapshot = payrollService.getEmployeeSnapshot(tenantId, employeeId, year, month);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    // ── Затваряне/отваряне на месец ──

    @PostMapping("/close")
    public ResponseEntity<MonthClosingSnapshot> closeMonth(@PathVariable String tenantId,
                                                             @RequestParam int year,
                                                             @RequestParam int month) {
        try {
            MonthClosingSnapshot snapshot = monthClosingService.closeMonth(tenantId, year, month);
            return ResponseEntity.ok(snapshot);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/reopen")
    public ResponseEntity<Void> reopenMonth(@PathVariable String tenantId,
                                              @RequestParam int year,
                                              @RequestParam int month) {
        monthClosingService.reopenMonth(tenantId, year, month);
        return ResponseEntity.ok().build();
    }

    // ── Преизчисляване на минал месец ──

    @PostMapping("/recalculate")
    public ResponseEntity<List<PayrollSnapshot>> recalculateMonth(@PathVariable String tenantId,
                                                                     @RequestParam int year,
                                                                     @RequestParam int month) {
        boolean wasClosed = monthClosingService.isMonthClosed(tenantId, year, month);
        if (wasClosed) {
            monthClosingService.reopenMonth(tenantId, year, month);
        }
        List<PayrollSnapshot> results = payrollService.calculateAll(tenantId, year, month);
        if (wasClosed) {
            monthClosingService.closeMonth(tenantId, year, month);
        }
        return ResponseEntity.ok(results);
    }

    // ── Справки ──

    @GetMapping("/reports/general")
    public ResponseEntity<PayrollReportService.PayrollReportData> generalReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getGeneralReport(tenantId, year, month));
    }

    @GetMapping("/reports/recap")
    public ResponseEntity<List<PayrollReportService.RecapLine>> recapReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getRecapReport(tenantId, year, month));
    }

    @GetMapping("/reports/by-department")
    public ResponseEntity<Map<String, PayrollReportService.PayrollReportData>> byDepartmentReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getByDepartmentReport(tenantId, year, month));
    }

    @GetMapping("/reports/insurance-income")
    public ResponseEntity<List<PayrollReportService.InsuranceIncomeRow>> insuranceIncomeReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getInsuranceIncomeReport(tenantId, year, month));
    }

    @GetMapping("/reports/comparison")
    public ResponseEntity<PayrollReportService.ComparisonReport> comparisonReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getComparisonReport(tenantId, year, month));
    }

    // ── Годишно приключване ──

    @PostMapping("/close-year")
    public ResponseEntity<YearClosingService.YearClosingResult> closeYear(
            @PathVariable String tenantId,
            @RequestParam int year) {
        return ResponseEntity.ok(yearClosingService.closeYear(tenantId, year));
    }

    @GetMapping("/reports/attendance")
    public ResponseEntity<PayrollReportService.AttendanceReport> attendanceReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getAttendanceReport(tenantId, year, month));
    }

    @GetMapping("/reports/statistics")
    public ResponseEntity<PayrollReportService.StatisticsReport> statisticsReport(
            @PathVariable String tenantId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reportService.getStatisticsReport(tenantId, year, month));
    }
}
