package com.valstrz.service;

import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.payroll.PayrollSnapshot.PayrollLine;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.MonthlyTimesheet;
import com.valstrz.repository.EmployeeRepository;
import com.valstrz.repository.MonthlyTimesheetRepository;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class PayrollReportService {

    private final PayrollService payrollService;
    private final MonthlyTimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;

    public PayrollReportService(PayrollService payrollService,
                                 MonthlyTimesheetRepository timesheetRepository,
                                 EmployeeRepository employeeRepository) {
        this.payrollService = payrollService;
        this.timesheetRepository = timesheetRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Обща ведомост - всички служители с колони за всяко перо.
     */
    public PayrollReportData getGeneralReport(String tenantId, int year, int month) {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);

        // Събираме уникални кодове на пера
        Set<String> codeSet = new LinkedHashSet<>();
        Map<String, String> codeNames = new LinkedHashMap<>();
        for (PayrollSnapshot s : snapshots) {
            if (s.getEarnings() != null) {
                for (PayrollLine line : s.getEarnings()) {
                    codeSet.add(line.getCode());
                    codeNames.put(line.getCode(), line.getName());
                }
            }
        }
        List<String> columnCodes = new ArrayList<>(codeSet);
        List<String> columnNames = new ArrayList<>();
        for (String code : columnCodes) {
            columnNames.add(codeNames.getOrDefault(code, code));
        }

        List<PayrollReportRow> rows = new ArrayList<>();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (PayrollSnapshot s : snapshots) {
            Map<String, Object> empData = s.getEmployeeData();
            String name = empData != null ? (String) empData.getOrDefault("fullName", "") : "";
            String dept = empData != null ? (String) empData.getOrDefault("departmentId", "") : "";

            Map<String, BigDecimal> values = new LinkedHashMap<>();
            if (s.getEarnings() != null) {
                for (PayrollLine line : s.getEarnings()) {
                    values.put(line.getCode(), line.getAmount());
                    totals.merge(line.getCode(), line.getAmount(), MoneyUtil::add);
                }
            }

            rows.add(new PayrollReportRow(
                    s.getEmployeeId(), name, dept, values,
                    s.getGrossSalary(), s.getTotalEmployeeInsurance(),
                    s.getIncomeTax(), s.getNetSalary()
            ));
        }

        return new PayrollReportData(columnCodes, columnNames, rows, totals);
    }

    /**
     * Рекапитулация - обобщени суми по код на перо.
     */
    public List<RecapLine> getRecapReport(String tenantId, int year, int month) {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);

        Map<String, RecapAccumulator> accMap = new LinkedHashMap<>();

        for (PayrollSnapshot s : snapshots) {
            accumulateLines(accMap, s.getEarnings());
            accumulateLines(accMap, s.getDeductions());
            accumulateLines(accMap, s.getEmployerContributions());
        }

        List<RecapLine> result = new ArrayList<>();
        for (Map.Entry<String, RecapAccumulator> entry : accMap.entrySet()) {
            RecapAccumulator acc = entry.getValue();
            result.add(new RecapLine(entry.getKey(), acc.name,
                    MoneyUtil.round(acc.total), acc.count));
        }
        return result;
    }

    private void accumulateLines(Map<String, RecapAccumulator> accMap, List<PayrollLine> lines) {
        if (lines == null) return;
        for (PayrollLine line : lines) {
            RecapAccumulator acc = accMap.computeIfAbsent(line.getCode(),
                    k -> new RecapAccumulator(line.getName()));
            acc.total = MoneyUtil.add(acc.total, line.getAmount());
            acc.count++;
        }
    }

    /**
     * Ведомост по отдели.
     */
    public Map<String, PayrollReportData> getByDepartmentReport(String tenantId, int year, int month) {
        PayrollReportData general = getGeneralReport(tenantId, year, month);

        Map<String, List<PayrollReportRow>> grouped = new LinkedHashMap<>();
        for (PayrollReportRow row : general.rows()) {
            String dept = row.department() != null && !row.department().isEmpty()
                    ? row.department() : "Без отдел";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(row);
        }

        Map<String, PayrollReportData> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<PayrollReportRow>> entry : grouped.entrySet()) {
            Map<String, BigDecimal> deptTotals = new LinkedHashMap<>();
            for (PayrollReportRow row : entry.getValue()) {
                for (Map.Entry<String, BigDecimal> v : row.values().entrySet()) {
                    deptTotals.merge(v.getKey(), v.getValue(), MoneyUtil::add);
                }
            }
            result.put(entry.getKey(), new PayrollReportData(
                    general.columnCodes(), general.columnNames(),
                    entry.getValue(), deptTotals));
        }
        return result;
    }

    /**
     * Справка за осигурителен доход по служители.
     */
    public List<InsuranceIncomeRow> getInsuranceIncomeReport(String tenantId, int year, int month) {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);
        List<InsuranceIncomeRow> rows = new ArrayList<>();

        for (PayrollSnapshot s : snapshots) {
            Map<String, Object> empData = s.getEmployeeData();
            String name = empData != null ? (String) empData.getOrDefault("fullName", "") : "";

            BigDecimal insurableIncome = s.getInsurableIncome() != null ? s.getInsurableIncome() : BigDecimal.ZERO;

            Map<String, Object> tsData = s.getTimesheetData();
            int workedDays = 0;
            int totalDays = 0;
            if (tsData != null) {
                workedDays = tsData.get("workedDays") instanceof Number n ? n.intValue() : 0;
                totalDays = tsData.get("totalInsuredDays") instanceof Number n ? n.intValue() : 0;
            }

            BigDecimal employeeIns = s.getTotalEmployeeInsurance() != null ? s.getTotalEmployeeInsurance() : BigDecimal.ZERO;
            BigDecimal employerIns = s.getTotalEmployerInsurance() != null ? s.getTotalEmployerInsurance() : BigDecimal.ZERO;

            rows.add(new InsuranceIncomeRow(
                    s.getEmployeeId(), name,
                    MoneyUtil.round(insurableIncome), workedDays, totalDays,
                    MoneyUtil.round(employeeIns), MoneyUtil.round(employerIns),
                    MoneyUtil.round(MoneyUtil.add(employeeIns, employerIns))
            ));
        }
        return rows;
    }

    /**
     * Сравнение текущ vs предходен месец.
     */
    public ComparisonReport getComparisonReport(String tenantId, int year, int month) {
        List<PayrollSnapshot> current = payrollService.getPayrollSnapshots(tenantId, year, month);

        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        List<PayrollSnapshot> previous = payrollService.getPayrollSnapshots(tenantId, prevYear, prevMonth);

        Map<String, PayrollSnapshot> prevMap = new LinkedHashMap<>();
        for (PayrollSnapshot s : previous) {
            prevMap.put(s.getEmployeeId(), s);
        }

        List<ComparisonRow> rows = new ArrayList<>();
        for (PayrollSnapshot cur : current) {
            Map<String, Object> empData = cur.getEmployeeData();
            String name = empData != null ? (String) empData.getOrDefault("fullName", "") : "";

            BigDecimal curGross = cur.getGrossSalary() != null ? cur.getGrossSalary() : BigDecimal.ZERO;
            BigDecimal curNet = cur.getNetSalary() != null ? cur.getNetSalary() : BigDecimal.ZERO;

            PayrollSnapshot prev = prevMap.get(cur.getEmployeeId());
            BigDecimal prevGross = BigDecimal.ZERO;
            BigDecimal prevNet = BigDecimal.ZERO;
            if (prev != null) {
                prevGross = prev.getGrossSalary() != null ? prev.getGrossSalary() : BigDecimal.ZERO;
                prevNet = prev.getNetSalary() != null ? prev.getNetSalary() : BigDecimal.ZERO;
            }

            rows.add(new ComparisonRow(
                    cur.getEmployeeId(), name,
                    MoneyUtil.round(curGross), MoneyUtil.round(prevGross),
                    MoneyUtil.round(curGross.subtract(prevGross)),
                    MoneyUtil.round(curNet), MoneyUtil.round(prevNet),
                    MoneyUtil.round(curNet.subtract(prevNet))
            ));
        }

        return new ComparisonReport(year, month, prevYear, prevMonth, rows);
    }

    /**
     * Обобщена статистика за месеца.
     */
    public StatisticsReport getStatisticsReport(String tenantId, int year, int month) {
        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);

        int headcount = snapshots.size();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalEmployeeIns = BigDecimal.ZERO;
        BigDecimal totalEmployerIns = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (PayrollSnapshot s : snapshots) {
            totalGross = MoneyUtil.add(totalGross, s.getGrossSalary());
            totalNet = MoneyUtil.add(totalNet, s.getNetSalary());
            totalEmployeeIns = MoneyUtil.add(totalEmployeeIns, s.getTotalEmployeeInsurance());
            totalEmployerIns = MoneyUtil.add(totalEmployerIns, s.getTotalEmployerInsurance());
            totalTax = MoneyUtil.add(totalTax, s.getIncomeTax());
        }

        BigDecimal avgGross = headcount > 0
                ? MoneyUtil.round(totalGross.divide(BigDecimal.valueOf(headcount), 6, java.math.RoundingMode.HALF_UP))
                : BigDecimal.ZERO;
        BigDecimal avgNet = headcount > 0
                ? MoneyUtil.round(totalNet.divide(BigDecimal.valueOf(headcount), 6, java.math.RoundingMode.HALF_UP))
                : BigDecimal.ZERO;

        BigDecimal totalCost = MoneyUtil.add(totalGross, totalEmployerIns);

        return new StatisticsReport(
                year, month, headcount,
                MoneyUtil.round(totalGross), MoneyUtil.round(totalNet),
                avgGross, avgNet,
                MoneyUtil.round(totalEmployeeIns), MoneyUtil.round(totalEmployerIns),
                MoneyUtil.round(totalTax), MoneyUtil.round(totalCost)
        );
    }

    // ── DTOs ──

    public record PayrollReportData(
        List<String> columnCodes,
        List<String> columnNames,
        List<PayrollReportRow> rows,
        Map<String, BigDecimal> totals
    ) {}

    public record PayrollReportRow(
        String employeeId, String employeeName, String department,
        Map<String, BigDecimal> values,
        BigDecimal gross, BigDecimal insurance, BigDecimal tax, BigDecimal net
    ) {}

    public record RecapLine(String code, String name, BigDecimal totalAmount, int employeeCount) {}

    public record InsuranceIncomeRow(
        String employeeId, String employeeName,
        BigDecimal insurableIncome, int workedDays, int totalInsuredDays,
        BigDecimal employeeInsurance, BigDecimal employerInsurance, BigDecimal totalInsurance
    ) {}

    public record ComparisonRow(
        String employeeId, String employeeName,
        BigDecimal currentGross, BigDecimal previousGross, BigDecimal grossDifference,
        BigDecimal currentNet, BigDecimal previousNet, BigDecimal netDifference
    ) {}

    public record ComparisonReport(
        int currentYear, int currentMonth, int previousYear, int previousMonth,
        List<ComparisonRow> rows
    ) {}

    public record StatisticsReport(
        int year, int month, int headcount,
        BigDecimal totalGross, BigDecimal totalNet,
        BigDecimal averageGross, BigDecimal averageNet,
        BigDecimal totalEmployeeInsurance, BigDecimal totalEmployerInsurance,
        BigDecimal totalIncomeTax, BigDecimal totalLaborCost
    ) {}

    // ── Присъствена ведомост ──

    public AttendanceReport getAttendanceReport(String tenantId, int year, int month) {
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        // Зареждаме всички timesheets
        Map<String, MonthlyTimesheet> tsMap = new LinkedHashMap<>();
        for (MonthlyTimesheet ts : timesheetRepository.findByTenantIdAndYearAndMonth(tenantId, year, month)) {
            tsMap.put(ts.getEmployeeId(), ts);
        }

        // Зареждаме имена на служители
        Map<String, String> nameMap = new LinkedHashMap<>();
        for (Employee emp : employeeRepository.findByTenantIdAndActive(tenantId, true)) {
            nameMap.put(emp.getId(), emp.getFullName());
        }

        List<AttendanceRow> rows = new ArrayList<>();
        for (Map.Entry<String, MonthlyTimesheet> entry : tsMap.entrySet()) {
            String empId = entry.getKey();
            MonthlyTimesheet ts = entry.getValue();
            String empName = nameMap.getOrDefault(empId, empId);

            List<String> dayCodes = new ArrayList<>();
            for (int d = 1; d <= daysInMonth; d++) {
                String code = "";
                if (ts.getDays() != null) {
                    for (MonthlyTimesheet.DailyEntry de : ts.getDays()) {
                        if (de.getDay() == d) {
                            if (de.getAbsenceCode() != null && !de.getAbsenceCode().isEmpty()) {
                                code = de.getAbsenceCode();
                            } else if ("WORK".equals(de.getDayType())) {
                                code = de.getWorkedHours() != null
                                        ? de.getWorkedHours().stripTrailingZeros().toPlainString()
                                        : "8";
                            } else if ("WEEKEND".equals(de.getDayType())) {
                                code = "П";
                            } else if ("HOLIDAY".equals(de.getDayType())) {
                                code = "Пр";
                            } else {
                                code = de.getDayType() != null ? de.getDayType() : "";
                            }
                            break;
                        }
                    }
                }
                dayCodes.add(code);
            }

            rows.add(new AttendanceRow(empId, empName, dayCodes,
                    ts.getTotalWorkedDays(), ts.getTotalAbsenceDays()));
        }

        return new AttendanceReport(year, month, daysInMonth, rows);
    }

    public record AttendanceReport(int year, int month, int daysInMonth, List<AttendanceRow> rows) {}
    public record AttendanceRow(String employeeId, String employeeName, List<String> dayCodes,
                                 int workedDays, int absenceDays) {}

    private static class RecapAccumulator {
        String name;
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        RecapAccumulator(String name) { this.name = name; }
    }
}
