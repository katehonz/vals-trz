package com.valstrz.service;

import com.valstrz.entity.calendar.MonthlyCalendar;
import com.valstrz.entity.calendar.WorkSchedule;
import com.valstrz.entity.company.Company;
import com.valstrz.entity.insurance.InsuranceContributions;
import com.valstrz.entity.insurance.InsuranceRates;
import com.valstrz.entity.insurance.InsuranceThreshold;
import com.valstrz.entity.payroll.Payroll;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.EmployeeDeduction;
import com.valstrz.entity.personnel.EmployeePayItem;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.entity.personnel.MonthlyTimesheet;
import com.valstrz.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PayrollService {

    private final PayrollCalculationService calculationService;
    private final PayrollRepository payrollRepository;
    private final PayrollSnapshotRepository snapshotRepository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentRepository employmentRepository;
    private final MonthlyTimesheetRepository timesheetRepository;
    private final MonthlyCalendarRepository calendarRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final InsuranceRatesRepository ratesRepository;
    private final InsuranceContributionsRepository contributionsRepository;
    private final InsuranceThresholdRepository thresholdRepository;
    private final CompanyRepository companyRepository;
    private final EmployeePayItemRepository employeePayItemRepository;
    private final EmployeeDeductionRepository employeeDeductionRepository;
    private final GarnishmentRepository garnishmentRepository;

    public PayrollService(PayrollCalculationService calculationService,
                           PayrollRepository payrollRepository,
                           PayrollSnapshotRepository snapshotRepository,
                           EmployeeRepository employeeRepository,
                           EmploymentRepository employmentRepository,
                           MonthlyTimesheetRepository timesheetRepository,
                           MonthlyCalendarRepository calendarRepository,
                           WorkScheduleRepository workScheduleRepository,
                           InsuranceRatesRepository ratesRepository,
                           InsuranceContributionsRepository contributionsRepository,
                           InsuranceThresholdRepository thresholdRepository,
                           CompanyRepository companyRepository,
                           EmployeePayItemRepository employeePayItemRepository,
                           EmployeeDeductionRepository employeeDeductionRepository,
                           GarnishmentRepository garnishmentRepository) {
        this.calculationService = calculationService;
        this.payrollRepository = payrollRepository;
        this.snapshotRepository = snapshotRepository;
        this.employeeRepository = employeeRepository;
        this.employmentRepository = employmentRepository;
        this.timesheetRepository = timesheetRepository;
        this.calendarRepository = calendarRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.ratesRepository = ratesRepository;
        this.contributionsRepository = contributionsRepository;
        this.thresholdRepository = thresholdRepository;
        this.companyRepository = companyRepository;
        this.employeePayItemRepository = employeePayItemRepository;
        this.employeeDeductionRepository = employeeDeductionRepository;
        this.garnishmentRepository = garnishmentRepository;
    }

    /**
     * Изчислява заплата за един служител (preview, не запазва).
     */
    public PayrollSnapshot calculateForEmployee(String tenantId, String employeeId, int year, int month) {
        Employee employee = findEmployee(tenantId, employeeId);
        Employment employment = findCurrentEmployment(tenantId, employeeId);
        if (employee == null || employment == null) {
            throw new IllegalArgumentException("Служителят или трудовото правоотношение не са намерени.");
        }

        PayrollCalculationService.CalculationInput input = buildInput(tenantId, employee, employment, year, month);
        return calculationService.calculate(input);
    }

    /**
     * Изчислява заплатите на всички активни служители и запазва snapshot-ите.
     */
    public List<PayrollSnapshot> calculateAll(String tenantId, int year, int month) {
        // Изтриваме стари snapshots за този месец (ако има)
        Iterable<PayrollSnapshot> existing = snapshotRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        snapshotRepository.deleteAll(existing);

        List<PayrollSnapshot> results = new ArrayList<>();
        java.math.BigDecimal totalGross = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalNet = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalEmployerCost = java.math.BigDecimal.ZERO;

        Iterable<Employee> activeEmployees = employeeRepository.findByTenantIdAndActive(tenantId, true);
        for (Employee employee : activeEmployees) {
            Employment employment = findCurrentEmployment(tenantId, employee.getId());
            if (employment == null) continue;

            MonthlyTimesheet ts = findTimesheet(tenantId, employee.getId(), year, month);
            if (ts == null) continue;

            try {
                PayrollCalculationService.CalculationInput input = buildInput(tenantId, employee, employment, year, month);
                PayrollSnapshot snapshot = calculationService.calculate(input);
                snapshotRepository.save(snapshot);
                results.add(snapshot);

                totalGross = totalGross.add(snapshot.getGrossSalary());
                totalNet = totalNet.add(snapshot.getNetSalary());
                totalEmployerCost = totalEmployerCost.add(snapshot.getTotalEmployerCost());
            } catch (Exception e) {
                // Логираме грешката, но продължаваме с останалите
                System.err.println("Грешка при изчисление за " + employee.getFullName() + ": " + e.getMessage());
            }
        }

        // Обновяваме статуса на ведомостта
        Payroll payroll = getOrCreatePayroll(tenantId, year, month);
        payroll.setStatus("CALCULATED");
        payroll.setCalculatedAt(LocalDateTime.now());
        payroll.setEmployeeCount(results.size());
        payroll.setTotalGross(totalGross);
        payroll.setTotalNet(totalNet);
        payroll.setTotalEmployerCost(totalEmployerCost);
        payrollRepository.save(payroll);

        return results;
    }

    public List<PayrollSnapshot> getPayrollSnapshots(String tenantId, int year, int month) {
        List<PayrollSnapshot> list = new ArrayList<>();
        snapshotRepository.findByTenantIdAndYearAndMonth(tenantId, year, month).forEach(list::add);
        return list;
    }

    public PayrollSnapshot getEmployeeSnapshot(String tenantId, String employeeId, int year, int month) {
        Iterable<PayrollSnapshot> snapshots = snapshotRepository.findByTenantIdAndEmployeeIdAndYearAndMonth(
                tenantId, employeeId, year, month);
        for (PayrollSnapshot s : snapshots) return s;
        return null;
    }

    public Payroll getOrCreatePayroll(String tenantId, int year, int month) {
        Iterable<Payroll> existing = payrollRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        for (Payroll p : existing) return p;

        Payroll payroll = new Payroll();
        payroll.setTenantId(tenantId);
        payroll.setYear(year);
        payroll.setMonth(month);
        payroll.setStatus("OPEN");
        payroll.setEmployeeCount(0);
        return payrollRepository.save(payroll);
    }

    // ── Helpers ──

    private PayrollCalculationService.CalculationInput buildInput(
            String tenantId, Employee employee, Employment employment, int year, int month) {

        MonthlyTimesheet ts = findTimesheet(tenantId, employee.getId(), year, month);
        if (ts == null) {
            throw new IllegalArgumentException("Няма часова карта за служител " + employee.getFullName());
        }

        MonthlyCalendar calendar = findCalendar(tenantId, year, month);
        if (calendar == null) {
            throw new IllegalArgumentException("Няма календар за " + year + "/" + month);
        }

        WorkSchedule ws = findWorkSchedule(tenantId, employment.getWorkScheduleCode());
        InsuranceRates rates = findRates(tenantId, year);
        if (rates == null) {
            throw new IllegalArgumentException("Няма осигурителни ставки за " + year);
        }

        String category = determineInsuranceCategory(employee.getEgn());
        String insuredType = employment.getInsuredType() != null ? employment.getInsuredType() : "01";
        InsuranceContributions contributions = findContributions(tenantId, year, category, insuredType);
        if (contributions == null) {
            throw new IllegalArgumentException("Няма осигурителни вноски за " + year + " / " + category + " / вид " + insuredType);
        }

        InsuranceThreshold threshold = findThreshold(tenantId, year, employment.getPersonnelGroup());

        // Зареждане на индивидуални пера за възнаграждение
        List<PayrollCalculationService.AdditionalEarning> earnings = new ArrayList<>();
        for (EmployeePayItem pi : employeePayItemRepository.findByTenantIdAndEmployeeId(tenantId, employee.getId())) {
            if (pi.isValidFor(year, month)) {
                earnings.add(new PayrollCalculationService.AdditionalEarning(
                        pi.getPayItemCode(), pi.getPayItemName(), pi.getType(), pi.getValue()));
            }
        }

        // Зареждане на индивидуални удръжки
        List<PayrollCalculationService.AdditionalDeduction> deductions = new ArrayList<>();
        for (EmployeeDeduction ed : employeeDeductionRepository.findByTenantIdAndEmployeeId(tenantId, employee.getId())) {
            if (ed.isValidFor(year, month)) {
                deductions.add(new PayrollCalculationService.AdditionalDeduction(
                        ed.getDeductionCode(), ed.getDeductionName(), ed.getAmount()));
            }
        }

        // Зареждане на индивидуални запори
        List<com.valstrz.entity.personnel.Garnishment> garnishments = new ArrayList<>();
        for (com.valstrz.entity.personnel.Garnishment g : garnishmentRepository.findByEmployeeId(employee.getId())) {
            if (g.isActive()) {
                garnishments.add(g);
            }
        }

        return new PayrollCalculationService.CalculationInput(
                employee, employment, ts, calendar, ws, rates, contributions, threshold,
                year, month, earnings, deductions, garnishments
        );
    }

    /**
     * Определя категорията (before1960/after1960) по ЕГН.
     * ЕГН формат: ГГММДД####
     * Месец 01-12 = 1900+, 21-32 = 1800+, 41-52 = 2000+
     */
    String determineInsuranceCategory(String egn) {
        if (egn == null || egn.length() < 4) return "after1960";
        try {
            int yy = Integer.parseInt(egn.substring(0, 2));
            int mm = Integer.parseInt(egn.substring(2, 4));
            int birthYear;
            if (mm > 40) {
                birthYear = 2000 + yy;
            } else if (mm > 20) {
                birthYear = 1800 + yy;
            } else {
                birthYear = 1900 + yy;
            }
            return birthYear <= 1960 ? "before1960" : "after1960";
        } catch (NumberFormatException e) {
            return "after1960";
        }
    }

    private Employee findEmployee(String tenantId, String employeeId) {
        return employeeRepository.findById(employeeId).orElse(null);
    }

    private Employment findCurrentEmployment(String tenantId, String employeeId) {
        Iterable<Employment> list = employmentRepository.findByTenantIdAndEmployeeIdAndCurrent(tenantId, employeeId, true);
        for (Employment e : list) return e;
        return null;
    }

    private MonthlyTimesheet findTimesheet(String tenantId, String employeeId, int year, int month) {
        Iterable<MonthlyTimesheet> list = timesheetRepository.findByTenantIdAndEmployeeIdAndYearAndMonth(
                tenantId, employeeId, year, month);
        for (MonthlyTimesheet ts : list) return ts;
        return null;
    }

    private MonthlyCalendar findCalendar(String tenantId, int year, int month) {
        Iterable<MonthlyCalendar> list = calendarRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        for (MonthlyCalendar c : list) return c;
        return null;
    }

    private WorkSchedule findWorkSchedule(String tenantId, String code) {
        if (code == null) return null;
        Iterable<WorkSchedule> list = workScheduleRepository.findByTenantId(tenantId);
        for (WorkSchedule ws : list) {
            if (code.equals(ws.getCode())) return ws;
        }
        return null;
    }

    private InsuranceRates findRates(String tenantId, int year) {
        Iterable<InsuranceRates> list = ratesRepository.findByTenantIdAndYear(tenantId, year);
        for (InsuranceRates r : list) return r;
        return null;
    }

    private InsuranceContributions findContributions(String tenantId, int year, String category, String insuredType) {
        // Try exact match by insuredType first
        Iterable<InsuranceContributions> list = contributionsRepository.findByTenantIdAndYearAndCategoryAndInsuredType(
                tenantId, year, category, insuredType);
        for (InsuranceContributions c : list) return c;
        // Fallback to type "01" (standard)
        if (!"01".equals(insuredType)) {
            list = contributionsRepository.findByTenantIdAndYearAndCategoryAndInsuredType(
                    tenantId, year, category, "01");
            for (InsuranceContributions c : list) return c;
        }
        // Legacy fallback: records without insuredType field
        list = contributionsRepository.findByTenantIdAndYearAndCategory(tenantId, year, category);
        for (InsuranceContributions c : list) return c;
        return null;
    }

    private InsuranceThreshold findThreshold(String tenantId, int year, int personnelGroup) {
        Iterable<InsuranceThreshold> list = thresholdRepository.findByTenantIdAndYearAndPersonnelGroup(
                tenantId, year, personnelGroup);
        for (InsuranceThreshold t : list) return t;
        return null;
    }
}
