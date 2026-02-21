package com.valstrz.controller;

import com.valstrz.entity.AuditLog;
import com.valstrz.entity.payroll.Payroll;
import com.valstrz.repository.*;
import com.valstrz.service.PersonnelAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER','VIEWER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/dashboard")
public class DashboardController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PayrollRepository payrollRepository;
    private final AbsenceRepository absenceRepository;
    private final AuditLogRepository auditLogRepository;
    private final PersonnelAnalyticsService analyticsService;

    public DashboardController(EmployeeRepository employeeRepository,
                               DepartmentRepository departmentRepository,
                               PayrollRepository payrollRepository,
                               AbsenceRepository absenceRepository,
                               AuditLogRepository auditLogRepository,
                               PersonnelAnalyticsService analyticsService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.payrollRepository = payrollRepository;
        this.absenceRepository = absenceRepository;
        this.auditLogRepository = auditLogRepository;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public Map<String, Object> getDashboardData(@PathVariable String tenantId) {
        Map<String, Object> data = new HashMap<>();

        // 1. Employee Count
        long employeeCount = StreamSupport.stream(
                employeeRepository.findByTenantIdAndActive(tenantId, true).spliterator(), false).count();
        data.put("employeeCount", employeeCount);

        // 2. Department Count
        long departmentCount = StreamSupport.stream(
                departmentRepository.findByTenantId(tenantId).spliterator(), false).count();
        data.put("departmentCount", departmentCount);

        // 3. Current Month/Year (Based on today)
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        data.put("currentMonthYear", year);
        data.put("currentMonthMonth", month);

        // 4. Payroll Info
        var payrolls = payrollRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        Iterator<Payroll> it = payrolls.iterator();
        if (it.hasNext()) {
            Payroll p = it.next();
            data.put("currentMonthStatus", p.getStatus());
            data.put("employeeCountCalculated", p.getEmployeeCount());
            data.put("calculatedAt", p.getCalculatedAt());
            
            // Note: Total sums might need to be aggregated from snapshots if not in Payroll entity
            // For now, let's assume they might be added or we just put placeholder/0 if not present
            // Based on earlier inspection of Payroll entity:
            // private BigDecimal totalGross; private BigDecimal totalNet; private BigDecimal totalEmployerCost;
            data.put("totalGross", p.getTotalGross() != null ? p.getTotalGross() : BigDecimal.ZERO);
            data.put("totalNet", p.getTotalNet() != null ? p.getTotalNet() : BigDecimal.ZERO);
            data.put("totalEmployerCost", p.getTotalEmployerCost() != null ? p.getTotalEmployerCost() : BigDecimal.ZERO);
        } else {
            data.put("currentMonthStatus", "NOT_STARTED");
            data.put("totalGross", BigDecimal.ZERO);
            data.put("totalNet", BigDecimal.ZERO);
            data.put("totalEmployerCost", BigDecimal.ZERO);
        }

        // 5. Pending Absences (for current month)
        long pendingAbsences = StreamSupport.stream(
                absenceRepository.findAll().spliterator(), false) // Need filtering by tenant + month
                .filter(a -> tenantId.equals(a.getTenantId()))
                .filter(a -> "REQUESTED".equals(a.getStatus()))
                .filter(a -> a.getFromDate() != null && a.getFromDate().getMonthValue() == month && a.getFromDate().getYear() == year)
                .count();
        data.put("pendingAbsences", pendingAbsences);

        // 6. Recent Audit Logs (Latest 5)
        List<AuditLog> recentLogs = StreamSupport.stream(
                auditLogRepository.findByTenantId(tenantId).spliterator(), false)
                .sorted((a, b) -> {
                    if (a.getPerformedAt() == null) return 1;
                    if (b.getPerformedAt() == null) return -1;
                    return b.getPerformedAt().compareTo(a.getPerformedAt());
                })
                .limit(5)
                .collect(Collectors.toList());
        data.put("recentAuditLogs", recentLogs);

        return data;
    }

    @GetMapping("/analytics")
    public PersonnelAnalyticsService.PersonnelReport getAnalytics(@PathVariable String tenantId) {
        return analyticsService.getReport(tenantId);
    }
}
